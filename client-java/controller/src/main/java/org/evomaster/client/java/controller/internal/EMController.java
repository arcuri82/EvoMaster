package org.evomaster.client.java.controller.internal;

import org.evomaster.client.java.controller.api.ControllerConstants;
import org.evomaster.client.java.controller.api.Formats;
import org.evomaster.client.java.controller.api.dto.*;
import org.evomaster.client.java.controller.api.dto.database.operations.DatabaseCommandDto;
import org.evomaster.client.java.controller.api.dto.mongo.FindOperationDto;
import org.evomaster.client.java.controller.api.dto.mongo.FindResultDto;
import org.evomaster.client.java.controller.api.dto.problem.RestProblemDto;
import org.evomaster.client.java.controller.db.QueryResult;
import org.evomaster.client.java.controller.db.SqlScriptRunner;
import org.evomaster.client.java.controller.mongo.DetailedFindResult;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.TargetInfo;
import org.evomaster.client.java.instrumentation.shared.StringSpecializationInfo;
import org.evomaster.client.java.utils.SimpleLogger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * Note: usually a RESTful webservice would be stateless.
 * Here, however, we have state. Reason is that we need it,
 * and the only client is the EvoMaster process. Furthermore,
 * the code of the controller should be as simple as possible,
 * as we might need to re-implement it in different languages.
 */
@Path("")
@Produces(Formats.JSON_V1)
public class EMController {

    private final SutController sutController;
    private String baseUrlOfSUT;

    /**
     * Keep track of all host:port clients connect so far.
     * This is the mainly done for debugging, to check that we are using
     * a single TCP connection, instead of creating new ones at each request.
     * <p>
     * However, we want to check it only during testing
     */
    private static final Set<String> connectedClientsSoFar = new CopyOnWriteArraySet<>();


    public EMController(SutController sutController) {
        this.sutController = Objects.requireNonNull(sutController);
    }

    private boolean trackRequestSource(HttpServletRequest request) {
        String source = request.getRemoteAddr() + ":" + request.getRemotePort();
        connectedClientsSoFar.add(source);
        return true;
    }

    /**
     * Only used for debugging/testing
     *
     * @return host:port of all clients connected so far
     */
    public static Set<String> getConnectedClientsSoFar() {
        return connectedClientsSoFar;
    }

    /**
     * Only used debugging/testing
     */
    public static void resetConnectedClientsSoFar() {
        connectedClientsSoFar.clear();
    }

    @Path(ControllerConstants.INFO_SUT_PATH)
    @GET
    public Response getSutInfo(@Context HttpServletRequest httpServletRequest) {

        String connectionHeader = httpServletRequest.getHeader("Connection");
        if (connectionHeader == null
                || !connectionHeader.equalsIgnoreCase("keep-alive")) {
            return Response.status(400).entity(WrappedResponseDto
                    .withError("Requests should always contain a 'Connection: keep-alive'")).build();
        }

        assert trackRequestSource(httpServletRequest);

        if (!sutController.verifySqlConnection()) {
            String msg = "SQL drivers are misconfigured. You must use a 'p6spy' wrapper when you " +
                    "run the SUT. For example, a database connection URL like 'jdbc:h2:mem:testdb' " +
                    "should be changed into 'jdbc:p6spy:h2:mem:testdb'. " +
                    "See documentation on how to configure P6Spy.";
            SimpleLogger.error(msg);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
        }

        SutInfoDto dto = new SutInfoDto();
        dto.isSutRunning = sutController.isSutRunning();
        dto.baseUrlOfSUT = baseUrlOfSUT;
        dto.infoForAuthentication = sutController.getInfoForAuthentication();
        dto.sqlSchemaDto = sutController.getSqlDatabaseSchema();
        dto.defaultOutputFormat = sutController.getPreferredOutputFormat();

        ProblemInfo info = sutController.getProblemInfo();
        if (info == null) {
            String msg = "Undefined problem type in the EM Controller";
            SimpleLogger.error(msg);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();

        } else if (info instanceof RestProblem) {
            RestProblem rp = (RestProblem) info;
            dto.restProblem = new RestProblemDto();
            dto.restProblem.swaggerJsonUrl = rp.getSwaggerJsonUrl();
            dto.restProblem.endpointsToSkip = rp.getEndpointsToSkip();

        } else {
            String msg = "Unrecognized problem type: " + info.getClass().getName();
            SimpleLogger.error(msg);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
        }

        dto.unitsInfoDto = sutController.getUnitsInfoDto();
        if (dto.unitsInfoDto == null) {
            String msg = "Failed to extract units info";
            SimpleLogger.error(msg);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
        }

        return Response.status(200).entity(WrappedResponseDto.withData(dto)).build();
    }

    @Path(ControllerConstants.CONTROLLER_INFO)
    @GET
    public Response getControllerInfoDto(@Context HttpServletRequest httpServletRequest) {

        assert trackRequestSource(httpServletRequest);

        ControllerInfoDto dto = new ControllerInfoDto();
        dto.fullName = sutController.getClass().getName();
        dto.isInstrumentationOn = sutController.isInstrumentationActivated();

        return Response.status(200).entity(WrappedResponseDto.withData(dto)).build();
    }

    @Path(ControllerConstants.NEW_SEARCH)
    @POST
    public Response newSearch(@Context HttpServletRequest httpServletRequest) {

        assert trackRequestSource(httpServletRequest);

        sutController.newSearch();

        return Response.status(201).entity(WrappedResponseDto.withNoData()).build();
    }


    @Path(ControllerConstants.RUN_SUT_PATH)
    @PUT
    @Consumes(Formats.JSON_V1)
    public Response runSut(SutRunDto dto, @Context HttpServletRequest httpServletRequest) {

        assert trackRequestSource(httpServletRequest);

        try {
            if (dto.run == null) {
                String msg = "Invalid JSON: 'run' field is required";
                SimpleLogger.warn(msg);
                return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
            }

            boolean sqlHeuristics = dto.calculateSqlHeuristics != null && dto.calculateSqlHeuristics;
            boolean sqlExecution = dto.extractSqlExecutionInfo != null && dto.extractSqlExecutionInfo;
            boolean mongoExecution = dto.extractMongoExecutionInfo != null && dto.extractMongoExecutionInfo;

            sutController.enableComputeSqlHeuristicsOrExtractExecution(sqlHeuristics, sqlExecution);
            sutController.enableExtractMongoExecutionInfo(mongoExecution);

            boolean doReset = dto.resetState != null && dto.resetState;

            synchronized (this) {

                if (!dto.run) {
                    if (doReset) {
                        String msg = "Invalid JSON: cannot reset state and stop service at same time";
                        SimpleLogger.warn(msg);
                        return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
                    }

                    //if on, we want to shut down the server
                    if (sutController.isSutRunning()) {
                        sutController.stopSut();
                        baseUrlOfSUT = null;
                    }

                } else {
                    /*
                        If SUT is not up and running, let's start it
                     */
                    if (!sutController.isSutRunning()) {
                        baseUrlOfSUT = sutController.startSut();
                        if (baseUrlOfSUT == null) {
                            //there has been an internal failure in starting the SUT
                            String msg = "Internal failure: cannot start SUT based on given configuration";
                            SimpleLogger.warn(msg);
                            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
                        }
                        sutController.initSqlHandler();
                        sutController.initMongoClient();
                    } else {
                        //TODO as starting should be blocking, need to check
                        //if initialized, and wait if not
                    }


                    /*
                        regardless of where it was running or not, need to reset state.
                        this is controlled by a boolean, although most likely we ll always
                        want to do it
                     */
                    if (dto.resetState != null && dto.resetState) {
                        sutController.resetStateOfSUT();
                        sutController.newTest();
                    }

                    /*
                        Note: here even if we start the SUT, the starting of a "New Search"
                        cannot be done here, as in this endpoint we also deal with the reset
                        of state. When we reset state for a new test run, we do not want to
                        reset all the other data regarding the whole search
                     */
                }
            }
        } catch (RuntimeException e) {
            /*
                FIXME: ideally, would not need to do a try/catch on each single endpoint,
                as could configure Jetty/Jackson to log all errors.
                But even after spending hours googling it, haven't managed to configure it
             */

            String msg = e.getMessage();
            SimpleLogger.error(msg, e);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
        }

        return Response.status(204).entity(WrappedResponseDto.withNoData()).build();
    }


    @Path(ControllerConstants.TEST_RESULTS)
    @GET
    public Response getTestResults(
            @QueryParam("ids")
            @DefaultValue("")
                    String idList,
            @Context HttpServletRequest httpServletRequest) {

        assert trackRequestSource(httpServletRequest);

        try {
            TestResultsDto dto = new TestResultsDto();

            Set<Integer> ids;

            try {
                ids = Arrays.stream(idList.split(","))
                        .filter(s -> !s.trim().isEmpty())
                        .map(Integer::parseInt)
                        .collect(Collectors.toSet());
            } catch (NumberFormatException e) {
                String msg = "Invalid parameter 'ids': " + e.getMessage();
                SimpleLogger.warn(msg);
                return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
            }

            List<TargetInfo> targetInfos = sutController.getTargetInfos(ids);
            if (targetInfos == null) {
                String msg = "Failed to collect target information for " + ids.size() + " ids";
                SimpleLogger.error(msg);
                return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
            }

            targetInfos.forEach(t -> {
                TargetInfoDto info = new TargetInfoDto();
                info.id = t.mappedId;
                info.value = t.value;
                info.descriptiveId = t.descriptiveId;
                info.actionIndex = t.actionIndex;

                dto.targets.add(info);
            });

            List<AdditionalInfo> additionalInfos = sutController.getAdditionalInfoList();
            if (additionalInfos != null) {
                additionalInfos.forEach(a -> {
                    AdditionalInfoDto info = new AdditionalInfoDto();
                    info.queryParameters = new HashSet<>(a.getQueryParametersView());
                    info.headers = new HashSet<>(a.getHeadersView());
                    info.lastExecutedStatement = a.getLastExecutedStatement();
                    info.rawAccessOfHttpBodyPayload = a.isRawAccessOfHttpBodyPayload();
                    info.parsedDtoNames = new HashSet<>(a.getParsedDtoNamesView());

                    info.stringSpecializations = new HashMap<>();
                    for (Map.Entry<String, Set<StringSpecializationInfo>> entry :
                            a.getStringSpecializationsView().entrySet()) {

                        assert !entry.getValue().isEmpty();

                        List<StringSpecializationInfoDto> list = entry.getValue().stream()
                                .map(it -> new StringSpecializationInfoDto(
                                        it.getStringSpecialization().toString(),
                                        it.getValue(),
                                        it.getType().toString()))
                                .collect(Collectors.toList());

                        info.stringSpecializations.put(entry.getKey(), list);
                    }

                    dto.additionalInfoList.add(info);
                });
            } else {
                String msg = "Failed to collect additional info";
                SimpleLogger.error(msg);
                return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
            }

            dto.extraHeuristics = sutController.getExtraHeuristics();

            return Response.status(200).entity(WrappedResponseDto.withData(dto)).build();

        } catch (RuntimeException e) {
            /*
                FIXME: ideally, would not need to do a try/catch on each single endpoint,
                as could configure Jetty/Jackson to log all errors.
                But even after spending hours googling it, haven't managed to configure it
             */

            String msg = "Thrown exception: " + e.getMessage();
            SimpleLogger.error(msg, e);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
        }
    }


    @Path(ControllerConstants.NEW_ACTION)
    @Consumes(MediaType.APPLICATION_JSON)
    @PUT
    public Response newAction(ActionDto dto, @Context HttpServletRequest httpServletRequest) {

        assert trackRequestSource(httpServletRequest);

        sutController.newAction(dto);

        return Response.status(204).entity(WrappedResponseDto.withNoData()).build();
    }


    @Path(ControllerConstants.MONGO_COMMAND)
    @Consumes(Formats.JSON_V1)
    @POST
    public Response executeMongoOperation(FindOperationDto dto, @Context HttpServletRequest httpServletRequest) {

        assert trackRequestSource(httpServletRequest);

        try {
            DetailedFindResult findResult = this.sutController.executeMongoFindOperation(dto);
            WrappedResponseDto<FindResultDto> wrappedResponse = WrappedResponseDto.withData(findResult.toDto());
            return Response.status(200).entity(wrappedResponse).build();

        } catch (RuntimeException e) {
            /*
                FIXME: ideally, would not need to do a try/catch on each single endpoint,
                as could configure Jetty/Jackson to log all errors.
                But even after spending hours googling it, haven't managed to configure it
             */

            String msg = "Thrown exception: " + e.getMessage();
            SimpleLogger.error(msg, e);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
        }

    }

    @Path(ControllerConstants.DATABASE_COMMAND)
    @Consumes(Formats.JSON_V1)
    @POST
    public Response executeDatabaseCommand(DatabaseCommandDto dto, @Context HttpServletRequest httpServletRequest) {

        assert trackRequestSource(httpServletRequest);

        try {
            Connection connection = sutController.getConnection();
            if (connection == null) {
                String msg = "No active database connection";
                SimpleLogger.warn(msg);
                return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
            }

            if (dto.command == null && (dto.insertions == null || dto.insertions.isEmpty())) {
                String msg = "No input command";
                SimpleLogger.warn(msg);
                return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
            }

            if (dto.command != null && dto.insertions != null && !dto.insertions.isEmpty()) {
                String msg = "Only 1 command can be specified";
                SimpleLogger.warn(msg);
                return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
            }

            if (dto.insertions != null) {
                if (dto.insertions.stream().anyMatch(i -> i.targetTable == null || i.targetTable.isEmpty())) {
                    String msg = "Insertion with no target table";
                    SimpleLogger.warn(msg);
                    return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
                }
            }

            QueryResult queryResult = null;
            Map<Long, Long> idMapping = null;

            try {
                if (dto.command != null) {
                    queryResult = SqlScriptRunner.execCommand(connection, dto.command);
                } else {
                    idMapping = SqlScriptRunner.execInsert(connection, dto.insertions);
                }
            } catch (Exception e) {
                String msg = "Failed to execute database command: " + e.getMessage();
                SimpleLogger.warn(msg);
                return Response.status(400).entity(WrappedResponseDto.withError(msg)).build();
            }

            if (queryResult != null) {
                return Response.status(200).entity(WrappedResponseDto.withData(queryResult.toDto())).build();
            } else if (idMapping != null) {
                return Response.status(200).entity(WrappedResponseDto.withData(idMapping)).build();
            } else {
                return Response.status(204).entity(WrappedResponseDto.withNoData()).build();
            }

        } catch (RuntimeException e) {
            /*
                FIXME: ideally, would not need to do a try/catch on each single endpoint,
                as could configure Jetty/Jackson to log all errors.
                But even after spending hours googling it, haven't managed to configure it
             */

            String msg = "Thrown exception: " + e.getMessage();
            SimpleLogger.error(msg, e);
            return Response.status(500).entity(WrappedResponseDto.withError(msg)).build();
        }
    }
}
