package org.evomaster.e2etests.spring.rest.mongo.foo;

import com.foo.mongo.MyMongoAppEmbeddedController;
import com.foo.mongo.person.PersonDto;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.evomaster.client.java.controller.api.dto.mongo.DocumentDto;
import org.evomaster.client.java.controller.api.dto.mongo.FindOperationDto;
import org.evomaster.client.java.controller.internal.db.StandardOutputTracker;
import org.evomaster.client.java.controller.mongo.DetailedFindResult;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.core.Main;
import org.evomaster.core.problem.rest.*;
import org.evomaster.core.problem.rest.auth.NoAuth;
import org.evomaster.core.problem.rest.param.Param;
import org.evomaster.core.problem.rest.param.PathParam;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.FitnessValue;
import org.evomaster.core.search.gene.DisruptiveGene;
import org.evomaster.core.search.gene.GeneIndependenceInfo;
import org.evomaster.core.search.gene.IntegerGene;
import org.evomaster.core.search.gene.StringGene;
import org.evomaster.core.search.service.FitnessFunction;
import org.evomaster.core.search.service.mutator.geneMutation.ArchiveMutator;
import org.evomaster.core.search.service.mutator.geneMutation.IntMutationUpdate;
import org.evomaster.e2etests.spring.rest.mongo.SpringRestMongoTestBase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExtraFitnessMongoFilterTest extends SpringRestMongoTestBase {

    private static final MyMongoAppEmbeddedController sutController = new MyMongoAppEmbeddedController();

    @BeforeAll
    public static void init() throws Exception {
        SpringRestMongoTestBase.initClass(sutController);
    }

    @BeforeEach
    public void turnOnTracker() {
        StandardOutputTracker.setTracker(true, sutController);

    }

    @AfterEach
    public void turnOffTracker() {
        StandardOutputTracker.setTracker(false, sutController);
    }

    private static ValidatableResponse add(PersonDto johnDoeDto) {
        return given()
                .contentType(ContentType.JSON)
                .body(johnDoeDto)
                .post(baseUrlOfSut + "/api/mongoperson/add")
                .then()
                .statusCode(200);
    }

    @Test
    public void testFindByAgeNoDocuments() {

        String[] args = buildEvoMasterArguments(1);

        Injector injector = Main.init(args);

        FitnessFunction<RestIndividual> ff = injector.getInstance(Key.get(
                new TypeLiteral<FitnessFunction<RestIndividual>>() {
                }));


        IntegerGene ageGene = new IntegerGene("age",
                0,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                new IntMutationUpdate(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, false));

        PathParam pathParam = new PathParam("age",
                new DisruptiveGene<>(
                        "age",
                        ageGene,
                        1.0d
                )
        );


        RestCallAction getAction = new RestCallAction("GET/api/mongoperson/findByAge/{age}",
                HttpVerb.GET,
                new RestPath("/api/mongoperson/findByAge/{age}"),
                Arrays.asList(pathParam),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestIndividual individual = new RestIndividual(Arrays.asList(getAction),
                SampleType.RANDOM,
                new LinkedList<>(),
                null,
                null);

        EvaluatedIndividual ei = ff.calculateCoverage(individual);
        FitnessValue fv = ei.getFitness();

        //as no data in database, should get distance greater than zero
        assertTrue(fv.averageExtraDistancesToMinimize(0) == Double.MAX_VALUE);

    }


    @NotNull
    private String[] buildEvoMasterArguments(int maxActionEvaluations) {
        ClassName unusedTestClassName = new ClassName("org.UnusedTestClassName");
        List<String> argsWithCompilation = this.getArgsWithCompilation(maxActionEvaluations, "unusedOutputFolder", unusedTestClassName);
        argsWithCompilation.addAll(Arrays.asList(
                "--heuristicsForMongo", "true",
                "--extractMongoExecutionInfo", "true",
                "--maxTestSize", String.valueOf(maxActionEvaluations)));
        return argsWithCompilation.toArray(new String[]{});
    }

    @Test
    public void testFindByAge() {

        String[] args = buildEvoMasterArguments(2);

        Injector injector = Main.init(args);

        FitnessFunction<RestIndividual> ff = injector.getInstance(Key.get(
                new TypeLiteral<FitnessFunction<RestIndividual>>() {
                }));

        IntegerGene ageGene = new IntegerGene("age",
                0,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                new IntMutationUpdate(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, false));

        PathParam pathParam = new PathParam("age",
                new DisruptiveGene<>(
                        "age",
                        ageGene,
                        1.0d
                )
        );

        RestCallAction postAction = new RestCallAction("POST/api/mongoperson/addJoeBlack",
                HttpVerb.POST, new RestPath("/api/mongoperson/addJoeBlack"),
                new LinkedList<>(),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestCallAction getAction = new RestCallAction("GET/api/mongoperson/findByAge/{age}",
                HttpVerb.GET,
                new RestPath("/api/mongoperson/findByAge/{age}"),
                Arrays.asList(pathParam),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestIndividual individual = new RestIndividual(Arrays.asList(postAction, getAction),
                SampleType.RANDOM,
                new LinkedList<>(),
                null,
                null);

        FindOperationDto findOperationDto = buildFindOperationDto("testdb", "person", "{}");
        sutController.executeMongoFindOperation(findOperationDto);

        DetailedFindResult findIterable0 = sutController.executeMongoFindOperation(findOperationDto);
        assertFalse(findIterable0.hasOperationFoundAnyDocuments());

        EvaluatedIndividual ei = ff.calculateCoverage(individual);
        FitnessValue fv = ei.getFitness();

        //as no data in database meets criterion, should get distance greater than zero
        assertTrue(fv.averageExtraDistancesToMinimize(1) > 0);

//        FindIterable<Document> findIterable1 = sutController.getMongoClient().getDatabase("testdb").getCollection("person").find();
//        assertFalse(findIterable1.iterator().hasNext());

    }

    @NotNull
    private static FindOperationDto buildFindOperationDto(String databaseName, String collectionName, String queryAsJsonString) {
        FindOperationDto findOperationDto = new FindOperationDto();
        findOperationDto.databaseName = databaseName;
        findOperationDto.collectionName = collectionName;
        DocumentDto documentDto = new DocumentDto();
        documentDto.documentAsJsonString = queryAsJsonString;
        findOperationDto.queryDocumentDto = documentDto;
        return findOperationDto;
    }


    @Test
    public void testFindByAgeBetween() {

        String[] args = buildEvoMasterArguments(2);

        Injector injector = Main.init(args);

        FitnessFunction<RestIndividual> ff = injector.getInstance(Key.get(
                new TypeLiteral<FitnessFunction<RestIndividual>>() {
                }));

        IntegerGene fromGene = new IntegerGene("from",
                0,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                new IntMutationUpdate(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, false));

        PathParam fromParam = new PathParam("from",
                new DisruptiveGene<>(
                        "from",
                        fromGene,
                        1.0d
                )
        );

        IntegerGene toGene = new IntegerGene("to",
                0,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                new IntMutationUpdate(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, false));

        PathParam toParam = new PathParam("to",
                new DisruptiveGene<IntegerGene>(
                        "to",
                        toGene,
                        1.0d
                )
        );

        RestCallAction postAction = new RestCallAction("POST/api/mongoperson/addJoeBlack",
                HttpVerb.POST, new RestPath("/api/mongoperson/addJoeBlack"),
                new LinkedList<>(),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestCallAction getAction = new RestCallAction("GET/api/mongoperson/findByAgeBetween/{from}/{to}",
                HttpVerb.GET,
                new RestPath("/api/mongoperson/findByAgeBetween/{from}/{to}"),
                Arrays.asList(fromParam, toParam),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestIndividual individual = new RestIndividual(Arrays.asList(postAction, getAction),
                SampleType.RANDOM,
                new LinkedList<>(),
                null,
                null);

        FindOperationDto dto = buildFindOperationDto("testdb", "person", "{}");
        DetailedFindResult findIterable0 = sutController.executeMongoFindOperation(dto);
        assertFalse(findIterable0.hasOperationFoundAnyDocuments());

        EvaluatedIndividual ei = ff.calculateCoverage(individual);
        FitnessValue fv = ei.getFitness();

        //as no data in database meets criterion, should get distance greater than zero
        assertTrue(fv.averageExtraDistancesToMinimize(1) > 0);

    }

    @Test
    public void testFindByLastName() {

        String[] args = buildEvoMasterArguments(2);

        Injector injector = Main.init(args);

        FitnessFunction<RestIndividual> ff = injector.getInstance(Key.get(
                new TypeLiteral<FitnessFunction<RestIndividual>>() {
                }));

        StringGene lastNameGene = new StringGene("lastName",
                "foo",
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                new LinkedList<>(),
                new LinkedList<>(),
                new IntMutationUpdate(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, false),
                new GeneIndependenceInfo(ArchiveMutator.WITHIN_NORMAL, 0, 0));

        PathParam lastNameParam = new PathParam("lastName",
                new DisruptiveGene<>(
                        "lastName",
                        lastNameGene,
                        1.0d
                )
        );

        RestCallAction postAction = new RestCallAction("POST/api/mongoperson/addJoeBlack",
                HttpVerb.POST, new RestPath("/api/mongoperson/addJoeBlack"),
                new LinkedList<Param>(),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestCallAction getAction = new RestCallAction("GET/api/mongoperson/findByLastName/{lastName}",
                HttpVerb.GET,
                new RestPath("/api/mongoperson/findByLastName/{lastName}"),
                Arrays.asList(lastNameParam),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestIndividual individual = new RestIndividual(Arrays.asList(postAction, getAction),
                SampleType.RANDOM,
                new LinkedList<>(),
                null,
                null);

        FindOperationDto dto = buildFindOperationDto("testdb", "person", "{}");
        DetailedFindResult findIterable0 = sutController.executeMongoFindOperation(dto);
        assertFalse(findIterable0.hasOperationFoundAnyDocuments());

        EvaluatedIndividual ei = ff.calculateCoverage(individual);
        FitnessValue fv = ei.getFitness();

        //as no data in database meets criterion, should get distance greater than zero
        assertTrue(fv.averageExtraDistancesToMinimize(1) > 0);

    }

    @Test
    public void testFindByAgeGreaterThan() {

        String[] args = buildEvoMasterArguments(2);

        Injector injector = Main.init(args);

        FitnessFunction<RestIndividual> ff = injector.getInstance(Key.get(
                new TypeLiteral<FitnessFunction<RestIndividual>>() {
                }));

        IntegerGene ageGene = new IntegerGene("age",
                100,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                new IntMutationUpdate(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, false));

        PathParam pathParam = new PathParam("age",
                new DisruptiveGene<>(
                        "age",
                        ageGene,
                        1.0d
                )
        );

        RestCallAction postAction = new RestCallAction("POST/api/mongoperson/addJoeBlack",
                HttpVerb.POST, new RestPath("/api/mongoperson/addJoeBlack"),
                new LinkedList<>(),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestCallAction getAction = new RestCallAction("GET/api/mongoperson/findByAgeGreaterThan/{age}",
                HttpVerb.GET,
                new RestPath("/api/mongoperson/findByAgeGreaterThan/{age}"),
                Arrays.asList(pathParam),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestIndividual individual = new RestIndividual(Arrays.asList(postAction, getAction),
                SampleType.RANDOM,
                new LinkedList<>(),
                null,
                null);

        FindOperationDto dto = buildFindOperationDto("testdb", "person", "{}");
        DetailedFindResult findIterable0 = sutController.executeMongoFindOperation(dto);
        assertFalse(findIterable0.hasOperationFoundAnyDocuments());

        EvaluatedIndividual ei = ff.calculateCoverage(individual);
        FitnessValue fv = ei.getFitness();

        //as no data in database meets criterion, should get distance greater than zero
        assertTrue(fv.averageExtraDistancesToMinimize(1) > 0);

    }

    @Test
    public void testFindByAgeLessThan() {

        String[] args = buildEvoMasterArguments(2);

        Injector injector = Main.init(args);

        FitnessFunction<RestIndividual> ff = injector.getInstance(Key.get(
                new TypeLiteral<FitnessFunction<RestIndividual>>() {
                }));

        IntegerGene ageGene = new IntegerGene("age",
                20,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                new IntMutationUpdate(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, false));

        PathParam pathParam = new PathParam("age",
                new DisruptiveGene<>(
                        "age",
                        ageGene,
                        1.0d
                )
        );

        RestCallAction postAction = new RestCallAction("POST/api/mongoperson/addJoeBlack",
                HttpVerb.POST, new RestPath("/api/mongoperson/addJoeBlack"),
                new LinkedList<>(),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestCallAction getAction = new RestCallAction("GET/api/mongoperson/findByAgeLessThan/{age}",
                HttpVerb.GET,
                new RestPath("/api/mongoperson/findByAgeLessThan/{age}"),
                Arrays.asList(pathParam),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestIndividual individual = new RestIndividual(Arrays.asList(postAction, getAction),
                SampleType.RANDOM,
                new LinkedList<>(),
                null,
                null);

        FindOperationDto dto = buildFindOperationDto("testdb", "person", "{}");
        DetailedFindResult findIterable0 = sutController.executeMongoFindOperation(dto);
        assertFalse(findIterable0.hasOperationFoundAnyDocuments());

        EvaluatedIndividual ei = ff.calculateCoverage(individual);
        FitnessValue fv = ei.getFitness();

        //as no data in database meets criterion, should get distance greater than zero
        assertTrue(fv.averageExtraDistancesToMinimize(1) > 0);

    }

    @Test
    public void testFindByFirstNameLike() {

        String[] args = buildEvoMasterArguments(2);

        Injector injector = Main.init(args);

        FitnessFunction<RestIndividual> ff = injector.getInstance(Key.get(
                new TypeLiteral<FitnessFunction<RestIndividual>>() {
                }));

        StringGene nameGene = new StringGene("name",
                "foo",
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                new LinkedList<>(),
                new LinkedList<>(),
                new IntMutationUpdate(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, false),
                new GeneIndependenceInfo(ArchiveMutator.WITHIN_NORMAL, 0, 0));

        PathParam nameParam = new PathParam("name",
                new DisruptiveGene<>(
                        "name",
                        nameGene,
                        1.0d
                )
        );

        RestCallAction postAction = new RestCallAction("POST/api/mongoperson/addJoeBlack",
                HttpVerb.POST, new RestPath("/api/mongoperson/addJoeBlack"),
                new LinkedList<Param>(),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestCallAction getAction = new RestCallAction("GET/api/mongoperson/findByFirstNameLike/{name}",
                HttpVerb.GET,
                new RestPath("/api/mongoperson/findByFirstNameLike/{name}"),
                Arrays.asList(nameParam),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestIndividual individual = new RestIndividual(Arrays.asList(postAction, getAction),
                SampleType.RANDOM,
                new LinkedList<>(),
                null,
                null);

        FindOperationDto dto = buildFindOperationDto("testdb", "person", "{}");
        DetailedFindResult findIterable0 = sutController.executeMongoFindOperation(dto);
        assertFalse(findIterable0.hasOperationFoundAnyDocuments());

        EvaluatedIndividual ei = ff.calculateCoverage(individual);
        FitnessValue fv = ei.getFitness();

        //as no data in database meets criterion, should get distance greater than zero
        assertTrue(fv.averageExtraDistancesToMinimize(1) > 0);

    }

    @Test
    public void testFindByFirstNameNotNull() {

        String[] args = buildEvoMasterArguments(2);

        Injector injector = Main.init(args);

        FitnessFunction<RestIndividual> ff = injector.getInstance(Key.get(
                new TypeLiteral<FitnessFunction<RestIndividual>>() {
                }));

        RestCallAction postAction = new RestCallAction("POST/api/mongoperson/addJoeBlack",
                HttpVerb.POST, new RestPath("/api/mongoperson/addJoeBlack"),
                new LinkedList<Param>(),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestCallAction getAction = new RestCallAction("GET/api/mongoperson/findByAddressNotNull",
                HttpVerb.GET,
                new RestPath("/api/mongoperson/findByAddressNotNull"),
                Arrays.asList(),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestIndividual individual = new RestIndividual(Arrays.asList(postAction, getAction),
                SampleType.RANDOM,
                new LinkedList<>(),
                null,
                null);

        FindOperationDto dto = buildFindOperationDto("testdb", "person", "{}");
        DetailedFindResult findIterable0 = sutController.executeMongoFindOperation(dto);
        assertFalse(findIterable0.hasOperationFoundAnyDocuments());

        EvaluatedIndividual ei = ff.calculateCoverage(individual);
        FitnessValue fv = ei.getFitness();

        //as no data in database meets criterion, should get distance greater than zero
        assertTrue(fv.averageExtraDistancesToMinimize(1) > 0);

    }

    @Test
    public void testFindByFirstNameNull() {

        String[] args = buildEvoMasterArguments(2);

        Injector injector = Main.init(args);

        FitnessFunction<RestIndividual> ff = injector.getInstance(Key.get(
                new TypeLiteral<FitnessFunction<RestIndividual>>() {
                }));

        RestCallAction postAction = new RestCallAction("POST/api/mongoperson/addJoeBlack",
                HttpVerb.POST, new RestPath("/api/mongoperson/addJoeBlack"),
                new LinkedList<Param>(),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestCallAction getAction = new RestCallAction("GET/api/mongoperson/findByFirstNameNull",
                HttpVerb.GET,
                new RestPath("/api/mongoperson/findByFirstNameNull"),
                Arrays.asList(),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestIndividual individual = new RestIndividual(Arrays.asList(postAction, getAction),
                SampleType.RANDOM,
                new LinkedList<>(),
                null,
                null);

        FindOperationDto dto = buildFindOperationDto("testdb", "person", "{}");
        DetailedFindResult findIterable0 = sutController.executeMongoFindOperation(dto);
        assertFalse(findIterable0.hasOperationFoundAnyDocuments());

        EvaluatedIndividual ei = ff.calculateCoverage(individual);
        FitnessValue fv = ei.getFitness();

        //as no data in database meets criterion, should get distance greater than zero
        assertTrue(fv.averageExtraDistancesToMinimize(1) > 0);

    }

    @Test
    public void testFindByFirstNameRegex() {

        String[] args = buildEvoMasterArguments(2);

        Injector injector = Main.init(args);

        FitnessFunction<RestIndividual> ff = injector.getInstance(Key.get(
                new TypeLiteral<FitnessFunction<RestIndividual>>() {
                }));

        StringGene nameGene = new StringGene("name",
                "foo",
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                new LinkedList<>(),
                new LinkedList<>(),
                new IntMutationUpdate(Integer.MIN_VALUE, Integer.MAX_VALUE, 0, false),
                new GeneIndependenceInfo(ArchiveMutator.WITHIN_NORMAL, 0, 0));

        PathParam nameParam = new PathParam("name",
                new DisruptiveGene<>(
                        "name",
                        nameGene,
                        1.0d
                )
        );

        RestCallAction postAction = new RestCallAction("POST/api/mongoperson/addJoeBlack",
                HttpVerb.POST, new RestPath("/api/mongoperson/addJoeBlack"),
                new LinkedList<Param>(),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestCallAction getAction = new RestCallAction("GET/api/mongoperson/findByFirstNameRegex/{name}",
                HttpVerb.GET,
                new RestPath("/api/mongoperson/findByFirstNameRegex/{name}"),
                Arrays.asList(nameParam),
                new NoAuth(),
                false,
                null,
                new LinkedList<>(),
                new HashMap<>());

        RestIndividual individual = new RestIndividual(Arrays.asList(postAction, getAction),
                SampleType.RANDOM,
                new LinkedList<>(),
                null,
                null);

        FindOperationDto dto = buildFindOperationDto("testdb", "person", "{}");
        DetailedFindResult findIterable0 = sutController.executeMongoFindOperation(dto);
        assertFalse(findIterable0.hasOperationFoundAnyDocuments());

        EvaluatedIndividual ei = ff.calculateCoverage(individual);
        FitnessValue fv = ei.getFitness();

        //as no data in database meets criterion, should get distance greater than zero
        assertTrue(fv.averageExtraDistancesToMinimize(1) > 0);

    }
}