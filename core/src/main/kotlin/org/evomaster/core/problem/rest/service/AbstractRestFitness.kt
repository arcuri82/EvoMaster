package org.evomaster.core.problem.rest.service

import com.google.inject.Inject
import org.bson.BsonDocument
import org.bson.Document
import org.bson.codecs.DecoderContext
import org.bson.codecs.DocumentCodec
import org.evomaster.client.java.controller.api.EMTestUtils
import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.client.java.controller.api.dto.HeuristicEntryDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.TestResultsDto
import org.evomaster.client.java.controller.api.dto.mongo.DocumentDto
import org.evomaster.client.java.controller.api.dto.mongo.FindOperationDto
import org.evomaster.core.database.DatabaseExecution
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.mongo.FindOperation
import org.evomaster.core.mongo.MongoExecution
import org.evomaster.core.mongo.MongoHeuristicCalculator
import org.evomaster.core.mongo.filter.ASTNodeFilter
import org.evomaster.core.mongo.filter.DocumentToASTFilterConverter
import org.evomaster.core.problem.rest.*
import org.evomaster.core.problem.rest.auth.NoAuth
import org.evomaster.core.problem.rest.param.BodyParam
import org.evomaster.core.problem.rest.param.HeaderParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.problem.rest.param.UpdateForBodyParam
import org.evomaster.core.remote.SutProblemException
import org.evomaster.core.remote.TcpUtils
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.ActionResult
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.ExtraHeuristicsLogger
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.SearchTimeController
import org.glassfish.jersey.client.ClientConfig
import org.glassfish.jersey.client.ClientProperties
import org.glassfish.jersey.client.HttpUrlConnectorProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.net.URL
import javax.annotation.PostConstruct
import javax.ws.rs.ProcessingException
import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.Entity
import javax.ws.rs.client.Invocation
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.NewCookie
import javax.ws.rs.core.Response


abstract class AbstractRestFitness<T> : FitnessFunction<T>() where T : Individual {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(AbstractRestFitness::class.java)
    }

    @Inject(optional = true)
    private lateinit var rc: RemoteController

    @Inject
    private lateinit var extraHeuristicsLogger: ExtraHeuristicsLogger

    @Inject
    private lateinit var searchTimeController: SearchTimeController

    private val clientConfiguration = ClientConfig()
            .property(ClientProperties.CONNECT_TIMEOUT, 10_000)
            .property(ClientProperties.READ_TIMEOUT, 10_000)
            //workaround bug in Jersey client
            .property(HttpUrlConnectorProvider.SET_METHOD_WORKAROUND, true)
            .property(ClientProperties.FOLLOW_REDIRECTS, false)


    private var client: Client = ClientBuilder.newClient(clientConfiguration)


    private lateinit var infoDto: SutInfoDto


    @PostConstruct
    private fun initialize() {

        log.debug("Initializing {}", AbstractRestFitness::class.simpleName)

        if (!config.blackBox || config.bbExperiments) {
            rc.checkConnection()

            val started = rc.startSUT()
            if (!started) {
                throw SutProblemException("Failed to start the system under test")
            }

            infoDto = rc.getSutInfo()
                    ?: throw SutProblemException("Failed to retrieve the info about the system under test")
        }

        log.debug("Done initializing {}", AbstractRestFitness::class.simpleName)
    }

    override fun reinitialize(): Boolean {

        try {
            if (!config.blackBox) {
                rc.stopSUT()
            }
            initialize()
        } catch (e: Exception) {
            log.warn("Failed to re-initialize the SUT: $e")
            return false
        }

        return true
    }

    protected fun handleExtra(dto: TestResultsDto, fv: FitnessValue) {
        handleSqlExtra(dto, fv)
        handleMongoExtra(dto, fv)
    }

    private fun handleSqlExtra(dto: TestResultsDto, fv: FitnessValue) {
        if (configuration.heuristicsForSQL) {

            for (i in 0 until dto.extraHeuristics.size) {

                val extra = dto.extraHeuristics[i]

                //TODO handling of toMaximize as well
                //TODO refactoring when will have other heuristics besides for SQL

                extraHeuristicsLogger.writeHeuristics(extra.heuristics, i)

                val toMinimize = extra.heuristics
                        .filter {
                            it != null
                                    && it.objective == HeuristicEntryDto.Objective.MINIMIZE_TO_ZERO
                        }.map { it.value }
                        .toList()

                if (!toMinimize.isEmpty()) {
                    fv.setExtraToMinimize(i, toMinimize)
                }

                fv.setDatabaseExecution(i, DatabaseExecution.fromDto(extra.databaseExecutionDto))
            }

            fv.aggregateDatabaseData()

            if (!fv.getViewOfAggregatedFailedWhere().isEmpty()) {
                searchTimeController.newIndividualsWithSqlFailedWhere()
            }
        } else if (configuration.extractSqlExecutionInfo) {

            for (i in 0 until dto.extraHeuristics.size) {
                val extra = dto.extraHeuristics[i]
                fv.setDatabaseExecution(i, DatabaseExecution.fromDto(extra.databaseExecutionDto))
            }
        }
    }

    private fun handleMongoExtra(dto: TestResultsDto, fv: FitnessValue) {
        if (configuration.heuristicsForMongo) {
            for (actionIndex in 0 until dto.extraHeuristics.size) {

                val extra = dto.extraHeuristics[actionIndex]

                val toMinimize = mutableListOf<Double>()
                extra.mongoExecutionDto?.executedFindOperationDtos?.forEach {

                    val distance =
                            if (it.findResultDto.hasReturnedAnyDocument) {
                                // Operation has found at least one document, therefore distance is 0.0
                                0.0
                            } else {
                                val findOperation = FindOperation.fromDto(it.findOperationDto)
                                // compute distance
                                computeMinDistance(findOperation.databaseName,
                                        findOperation.collectionName,
                                        findOperation.queryDocument)
                            }
                    toMinimize += distance
                }
                if (toMinimize.isNotEmpty()) {
                    fv.setExtraToMinimize(actionIndex, toMinimize)
                }
            }
        }
        if (configuration.extractMongoExecutionInfo) {
            for (i in 0 until dto.extraHeuristics.size) {
                val mongoExecutionDto = dto.extraHeuristics[i].mongoExecutionDto
                fv.setMongoExecution(i, MongoExecution.fromDto(mongoExecutionDto))
            }
        }
    }

    private fun computeMinDistance(databaseName: String, collectionName: String, filter: Document): Double {

        val findAllDocumentsDto = FindOperationDto()
        findAllDocumentsDto.databaseName = databaseName
        findAllDocumentsDto.collectionName = collectionName
        val documentDto = DocumentDto()
        documentDto.documentAsJsonString= "{}"
        findAllDocumentsDto.queryDocumentDto = documentDto

        val retrievedDocumentsDto = this.rc.executeMongoOperationAndGetQueryResults(findAllDocumentsDto)

        val filterNode = toASTNodeFilter(filter)

        if (filterNode == null) {
            // query was not supported
            return Double.MAX_VALUE
        } else {

            if (retrievedDocumentsDto?.documents.isNullOrEmpty()) {
                // Collection is empty
                return Double.MAX_VALUE
            } else {
                // collection is not empty
                return retrievedDocumentsDto
                        ?.documents
                        ?.toList()
                        ?.map{
                            BsonDocument.parse(it.documentAsJsonString)
                        }
                        ?.map{
                            DocumentCodec().decode(it.asBsonReader(), DecoderContext.builder().build())
                        }
                        ?.map {
                            filterNode.accept(MongoHeuristicCalculator(), it)
                        }
                        ?.toList()
                        ?.min()!!
            }
        }
    }

    private fun toASTNodeFilter(filter: Document): ASTNodeFilter? {
        try {
            return DocumentToASTFilterConverter().translate(filter)
        } catch (ex: IllegalArgumentException) {
            log.warn("Could not translate document $filter")
            return null
        }
    }

    /**
     * Based on what executed by the test, we might need to add new genes to the individual.
     * This for example can happen if we detected that the test is using headers or query
     * params that were not specified in the Swagger schema
     */
    open fun expandIndividual(
            individual: RestIndividual,
            additionalInfoList: List<AdditionalInfoDto>,
            actionResults: List<ActionResult>
    ) {

        if (individual.seeActions().size < additionalInfoList.size) {
            /*
                Note: as not all actions might had been executed, it might happen that
                there are less Info than declared actions.
                But the other way round should not really happen
             */
            log.warn("Length mismatch between ${individual.seeActions().size} actions and ${additionalInfoList.size} info data")
            return
        }

        for (i in 0 until additionalInfoList.size) {

            val action = individual.seeActions()[i]
            val info = additionalInfoList[i]

            if (action !is RestCallAction) {
                continue
            }

            val result = actionResults[i] as RestCallResult

            /*
                Those are OptionalGenes, which MUST be off by default.
                We are changing the genotype, but MUST not change the phenotype.
                Otherwise, the fitness value we just computed would be wrong.

                TODO: should update default action templates in Sampler
             */

            info.headers
                    .filter { name ->
                        !action.parameters.any { it is HeaderParam && it.name.equals(name, ignoreCase = true) }
                    }
                    .forEach {
                        action.parameters.add(HeaderParam(it, OptionalGene(it, StringGene(it), false)))
                    }

            info.queryParameters
                    .filter { name ->
                        !action.parameters.any { it is QueryParam && it.name.equals(name, ignoreCase = true) }
                    }
                    .forEach { name ->
                        action.parameters.add(QueryParam(name, OptionalGene(name, StringGene(name), false)))
                    }

            if(result.getStatusCode() == 415){
                /*
                    In theory, this should not happen.
                    415 means the media type of the sent payload is wrong.
                    There is no point for EvoMaster to do that, ie sending an XML to
                    an endpoint that expects a JSON.
                    Having such kind of test would be pretty pointless.

                    However, a POST/PUT could expect a payload and, if that is not specified
                    in OpenAPI, we could get a 415 when sending no data.
                 */
                if(action.parameters.none{ it is BodyParam}){

                    val obj = ObjectGene("body", listOf())

                    val body = BodyParam(obj,
                             // TODO could look at "Accept" header instead of defaulting to JSON
                            EnumGene("contentType", listOf("application/json")))

                    val update = UpdateForBodyParam(body)

                    action.parameters.add(update)
                }
            }


            val dtoNames = info.parsedDtoNames;

            val noBody = action.parameters.none{ it is BodyParam}
            val emptyObject = !noBody &&
                    // this is the case of 415 handling
                    action.parameters.find { it is BodyParam }!!.let {
                        it.gene is ObjectGene && it.gene.fields.isEmpty()
                    }

            if(info.rawAccessOfHttpBodyPayload
                    && dtoNames.isNotEmpty()
                    && (noBody || emptyObject)
            ){
                /*
                    The SUT tried to read the HTTP body payload, but there is no info
                    about it in the schema. This can happen when payloads are dynamically
                    loaded directly in the business logic of the SUT, and automated tools
                    like SpringDoc/SpringFox failed to infer what is read

                    TODO could handle other types besides JSON
                    TODO what to do if more than 1 DTO are registered?
                         Likely need a new MultiOptionGene similar to DisjunctionListRxGene
                 */
                if(dtoNames.size > 1){
                    LoggingUtil.uniqueWarn(log, "More than 1 DTO option: [${dtoNames.sorted().joinToString(", ")}]")
                }
                val name = dtoNames.first()
                val obj = getObjectGeneForDto(name)

                val body = BodyParam(obj, EnumGene("contentType", listOf("application/json")))
                val update = UpdateForBodyParam(body)
                action.parameters.add(update)
            }
        }
    }

    private fun getObjectGeneForDto(name: String) : Gene{

        if(!infoDto.unitsInfoDto.parsedDtos.containsKey(name)){
            /*
                parsedDto info is update throughout the search.
                so, if info is missing, we re-fetch the whole data.
                Would be more efficient to just fetch new data, but,
                as this will happens seldom (at most N times for N dtos),
                no much point in optimizing it
             */
            infoDto = rc.getSutInfo()!!

            if(!infoDto.unitsInfoDto.parsedDtos.containsKey(name)){
                throw RuntimeException("BUG: info for DTO $name is not available in the SUT driver")
            }
        }

        val schema : String = infoDto.unitsInfoDto.parsedDtos.get(name)!!

        return RestActionBuilderV3.createObjectGeneForDTO(name, schema)
    }

    /**
     * Initializing Actions before evaluating its fitness if need
     */
    open fun doInitializingActions(ind: T) {
    }

    /**
     * Create local targets for each HTTP status code in each
     * API entry point
     */
    protected fun handleResponseTargets(
            fv: FitnessValue,
            actions: List<RestAction>,
            actionResults: List<ActionResult>,
            additionalInfoList: List<AdditionalInfoDto>) {

        (0 until actionResults.size)
                .filter { actions[it] is RestCallAction }
                .filter { actionResults[it] is RestCallResult }
                .forEach {
                    val result = actionResults[it] as RestCallResult
                    val status = result.getStatusCode() ?: -1
                    val name = actions[it].getName()

                    //objective for HTTP specific status code
                    val statusId = idMapper.handleLocalTarget("$status:$name")
                    fv.updateTarget(statusId, 1.0, it)

                    /*
                        Objectives for results on endpoints.
                        Problem: we might get a 4xx/5xx, but then no gradient to keep sampling for
                        that endpoint. If we get 2xx, and full coverage, then no gradient to try
                        to keep sampling that endpoint to get a 5xx
                     */
                    val okId = idMapper.handleLocalTarget("HTTP_SUCCESS:$name")
                    val faultId = idMapper.handleLocalTarget("HTTP_FAULT:$name")

                    //OK -> 5xx being better than 4xx, as code executed
                    //FAULT -> 4xx worse than 2xx (can't find bugs if input is invalid)
                    if (status in 200..299) {
                        fv.updateTarget(okId, 1.0, it)
                        fv.updateTarget(faultId, 0.5, it)
                    } else if (status in 400..499) {
                        fv.updateTarget(okId, 0.1, it)
                        fv.updateTarget(faultId, 0.1, it)
                    } else if (status in 500..599) {
                        fv.updateTarget(okId, 0.5, it)
                        fv.updateTarget(faultId, 1.0, it)
                    }

                    /*
                        500 codes "might" be bugs. To distinguish between different bugs
                        that crash the same endpoint, we need to know what was the last
                        executed statement in the SUT.
                        So, we create new targets for it.
                     */
                    if (status == 500) {
                        val statement = additionalInfoList[it].lastExecutedStatement
                        val source = statement ?: "framework_code"
                        val descriptiveId = idMapper.getFaultDescriptiveId("$source $name")
                        val bugId = idMapper.handleLocalTarget(descriptiveId)
                        fv.updateTarget(bugId, 1.0, it)
                        result.setLastStatementWhen500(source)
                    }
                }
    }


    private fun getBaseUrl(): String {
        var baseUrl = if (!config.blackBox || config.bbExperiments) {
            infoDto.baseUrlOfSUT
        } else {
            BlackBoxUtils.restUrl(config)
        }

        try{
            /*
                Note: this in theory should already been checked: either in EMConfig for
                Black-box testing, or already in the driver for White-Box testing
             */
            URL(baseUrl)
        } catch (e: MalformedURLException){
            val base = "Invalid 'baseUrl'."
            val wb = "In the EvoMaster driver, in the startSut() method, you must make sure to return a valid URL."
            val err = " ERROR: $e"

            val msg = if(config.blackBox) "$base $err" else "$base $wb $err"
            throw SutProblemException(msg)
        }

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length - 1)
        }

        return baseUrl
    }

    /**
     * @return whether the call was OK. Eg, in some cases, we might want to stop
     * the test at this action, and do not continue
     */
    protected fun handleRestCall(a: RestCallAction,
                                 actionResults: MutableList<ActionResult>,
                                 chainState: MutableMap<String, String>,
                                 cookies: Map<String, List<NewCookie>>)
            : Boolean {

        val rcr = RestCallResult()
        actionResults.add(rcr)

        val response = try {
            createInvocation(a, chainState, cookies).invoke()
        } catch (e: ProcessingException) {

            /*
                this could have happened for example if call ends up in an infinite redirection loop.
                However, as we no longer follow 3xx automatically, it should not happen anymore
             */
            when {
                TcpUtils.isTooManyRedirections(e) -> {
                    rcr.setInfiniteLoop(true)
                    rcr.setErrorMessage(e.cause!!.message!!)
                    return false
                }
                TcpUtils.isTimeout(e) -> {
                    /*
                        This is very tricky. In theory it shouldn't happen that a REST call
                        does timeout (eg 10 seconds). But it might happen due to glitch,
                        or if very slow hardware. If it is a glitch, we do not want to
                        kill whole EM process, as might not happen again. If it is a
                        constant, we want to avoid using such test if possible, as it
                        would kill performance.
                        In any case, a generated test should never check assertions on time,
                        eg expect that a is SocketTimeoutException thrown. Not only because
                        maybe it was just a glitch, but also because the test might be run
                        on different machines (remote CI vs local development PC) with
                        different performance (and so the test would become flaky)
                     */
                    rcr.setTimedout(true)
                    statistics.reportTimeout()
                    return false
                }
                TcpUtils.isOutOfEphemeralPorts(e) -> {
                    /*
                        This could happen if for any reason we run out of ephemeral ports.
                        In such a case, we wait X seconds, and try again, as OS might have released ports
                        meanwhile.
                        And while we are at it, let's release any hanging network resource
                     */
                    client.close() //make sure to release any resource
                    client = ClientBuilder.newClient()

                    val seconds = 30
                    log.warn("Running out of ephemeral ports. Waiting $seconds seconds before re-trying connection")
                    Thread.sleep(seconds * 1000L)

                    createInvocation(a, chainState, cookies).invoke()

                }
                else -> throw e
            }
        }

        rcr.setStatusCode(response.status)

        try {
            if (response.hasEntity()) {
                if (response.mediaType != null) {
                    rcr.setBodyType(response.mediaType)
                }
                /*
                    FIXME should read as byte[]
                 */
                val body = response.readEntity(String::class.java)

                if (body.length < configuration.maxResponseByteSize) {
                    rcr.setBody(body)
                } else {
                    LoggingUtil.uniqueWarn(log,
                            "A very large response body was retrieved from the endpoint '${a.path}'." +
                                    " If that was expected, increase the 'maxResponseByteSize' threshold" +
                                    " in the configurations.")
                    rcr.setTooLargeBody(true)
                }
            }
        } catch (e: Exception) {

            if (e is ProcessingException && TcpUtils.isTimeout(e)) {
                rcr.setTimedout(true)
                statistics.reportTimeout()
                return false
            } else {
                log.warn("Failed to parse HTTP response: ${e.message}")
            }
        }

        if (response.status == 401 && a.auth !is NoAuth) {
            //this would likely be a misconfiguration in the SUT controller
            log.warn("Got 401 although having auth for '${a.auth.name}'")
        }


        if (!handleSaveLocation(a, response, rcr, chainState)) return false

        return true
    }

    private fun createInvocation(a: RestCallAction, chainState: MutableMap<String, String>, cookies: Map<String, List<NewCookie>>): Invocation {
        val baseUrl = getBaseUrl()

        val path = a.resolvedPath()

        val locationHeader = if (a.locationId != null) {
            chainState[locationName(a.locationId!!)]
                    ?: throw IllegalStateException("Call expected a missing chained 'location'")
        } else {
            null
        }

        val fullUri = EMTestUtils.resolveLocation(locationHeader, baseUrl + path)!!
                .let {
                    /*
                        TODO this will be need to be done properly, and check if
                        it is or not a valid char.
                        Furthermore, likely needed to be done in resolveLocation,
                        or at least check how RestAssured would behave
                     */
                    //it.replace("\"", "")
                    GeneUtils.applyEscapes(it, GeneUtils.EscapeMode.URI, configuration.outputFormat)
                }


        val builder = if (a.produces.isEmpty()) {
            log.debug("No 'produces' type defined for {}", path)
            client.target(fullUri).request("*/*")

        } else {
            /*
                TODO: This only considers the first in the list of produced responses
                This is fine for endpoints that only produce one type of response.
                Could be a problem in future
            */
            client.target(fullUri).request(a.produces.first())
        }

        a.auth.headers.forEach {
            builder.header(it.name, it.value)
        }

        val prechosenAuthHeaders = a.auth.headers.map { it.name }

        /*
            TODO: optimization, avoid mutating header gene if anyway
            using pre-chosen one
         */

        a.parameters.filterIsInstance<HeaderParam>()
                .filter { !prechosenAuthHeaders.contains(it.name) }
                .forEach {
                    builder.header(it.name, it.gene.getValueAsRawString())
                }

        if (a.auth.cookieLogin != null) {
            val list = cookies[a.auth.cookieLogin!!.username]
            if (list == null || list.isEmpty()) {
                log.warn("No cookies for ${a.auth.cookieLogin!!.username}")
            } else {
                list.forEach {
                    builder.cookie(it.toCookie())
                }
            }
        }

        /*
            TODO: need to handle "accept" of returned resource
         */


        val body = a.parameters.find { p -> p is BodyParam }
        val forms = a.getBodyFormData()

        if (body != null && forms != null) {
            throw IllegalStateException("Issue in OpenAPI configuration: both Body and FormData definitions in the same endpoint")
        }

        val bodyEntity = if (body != null && body is BodyParam) {
            val mode = when {
                body.isJson() -> GeneUtils.EscapeMode.JSON
                body.isXml() -> GeneUtils.EscapeMode.XML
                body.isForm() -> GeneUtils.EscapeMode.X_WWW_FORM_URLENCODED
                body.isTextPlain() -> GeneUtils.EscapeMode.TEXT
                else -> throw IllegalStateException("Cannot handle body type: " + body.contentType())
            }
            Entity.entity(body.gene.getValueAsPrintableString(mode = mode, targetFormat = configuration.outputFormat), body.contentType())
        } else if (forms != null) {
            Entity.entity(forms, MediaType.APPLICATION_FORM_URLENCODED_TYPE)
        } else if (a.verb == HttpVerb.PUT || a.verb == HttpVerb.PATCH) {
            /*
                PUT and PATCH must have a payload. But it might happen that it is missing in the Swagger schema
                when objects like WebRequest are used. So we default to urlencoded
             */
            Entity.entity("", MediaType.APPLICATION_FORM_URLENCODED_TYPE)
        } else {
            null
        }

        val invocation = when (a.verb) {
            HttpVerb.GET -> builder.buildGet()
            HttpVerb.POST -> builder.buildPost(bodyEntity)
            HttpVerb.PUT -> builder.buildPut(bodyEntity)
            HttpVerb.DELETE -> builder.buildDelete()
            HttpVerb.PATCH -> builder.build("PATCH", bodyEntity)
            HttpVerb.OPTIONS -> builder.build("OPTIONS")
            HttpVerb.HEAD -> builder.build("HEAD")
            HttpVerb.TRACE -> builder.build("TRACE")
        }
        return invocation
    }

    private fun handleSaveLocation(a: RestCallAction, response: Response, rcr: RestCallResult, chainState: MutableMap<String, String>): Boolean {
        if (a.saveLocation) {

            if (response.statusInfo.family != Response.Status.Family.SUCCESSFUL) {
                /*
                    If this failed, and following actions require the "location" header
                    of this call, there is no point whatsoever to continue evaluating
                    the remaining calls
                 */
                rcr.stopping = true
                return false
            }

            val name = locationName(a.path.lastElement())
            var location = response.getHeaderString("location")

            if (location == null) {
                /*
                    Excluding bugs, this might happen if API was
                    designed to return the created resource, from
                    which an "id" can be extracted.
                    This is usually not a good practice, but it can
                    happen. So, here we "heuristically" (cannot be 100% sure)
                    check if this is indeed the case
                 */
                val id = rcr.getResourceId()

                if (id != null && hasParameterChild(a)) {
                    location = a.resolvedPath() + "/" + id
                    rcr.setHeuristicsForChainedLocation(true)
                }
            }

            //save location for the following REST calls
            chainState[name] = location ?: ""
        }
        return true
    }


    abstract fun hasParameterChild(a: RestCallAction): Boolean

    private fun locationName(id: String): String {
        return "location_$id"
    }

    /**
     * If any action needs auth based on cookies, do a "login" before
     * running the actions, and collect the cookies from the server.
     *
     * @return a map from username to auth cookie for those users
     */
    protected fun getCookies(ind: RestIndividual): Map<String, List<NewCookie>> {

        val cookieLogins = ind.getCookieLoginAuth()

        val map: MutableMap<String, List<NewCookie>> = HashMap()

        val baseUrl = getBaseUrl()

        for (cl in cookieLogins) {

            val mediaType = when (cl.contentType) {
                ContentType.X_WWW_FORM_URLENCODED -> MediaType.APPLICATION_FORM_URLENCODED_TYPE
                ContentType.JSON -> MediaType.APPLICATION_JSON_TYPE
            }

            val response = try {
                client.target(baseUrl + cl.loginEndpointUrl)
                        .request()
                        //TODO could consider other cases besides POST
                        .buildPost(Entity.entity(cl.payload(), mediaType))
                        .invoke()
            } catch (e: Exception) {
                log.warn("Failed to login for ${cl.username}/${cl.password}: $e")
                continue
            }

            if (response.statusInfo.family != Response.Status.Family.SUCCESSFUL) {

                /*
                    if it is a 3xx, we need to look at Location header to determine
                    if a success or failure.
                    TODO: could explicitly ask for this info in the auth DTO.
                    However, as 3xx makes little sense in a REST API, maybe not so
                    important right now, although had this issue with some APIs using
                    default settings in Spring Security
                */
                if (response.statusInfo.family == Response.Status.Family.REDIRECTION) {
                    val location = response.getHeaderString("location")
                    if (location != null && (location.contains("error", true) || location.contains("login", true))) {
                        log.warn("Login request failed with ${response.status} redirection toward $location")
                        continue
                    }
                } else {
                    log.warn("Login request failed with status ${response.status}")
                    continue
                }
            }

            if (response.cookies.isEmpty()) {
                log.warn("Cookie-based login request did not give back any new cookie")
                continue
            }

            map[cl.username] = response.cookies.values.toList()
        }

        return map
    }
}