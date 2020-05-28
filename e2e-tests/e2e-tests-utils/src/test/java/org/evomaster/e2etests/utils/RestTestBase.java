package org.evomaster.e2etests.utils;

import kotlin.Unit;
import org.apache.commons.io.FileUtils;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.InstrumentedSutStarter;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.internal.SutController;
import org.evomaster.client.java.instrumentation.shared.ClassName;
import org.evomaster.core.Main;
import org.evomaster.core.StaticCounter;
import org.evomaster.core.logging.LoggingUtil;
import org.evomaster.core.output.OutputFormat;
import org.evomaster.core.output.compiler.CompilerForTestGenerated;
import org.evomaster.core.problem.rest.*;
import org.evomaster.core.remote.service.RemoteController;
import org.evomaster.core.search.Action;
import org.evomaster.core.search.EvaluatedIndividual;
import org.evomaster.core.search.Individual;
import org.evomaster.core.search.Solution;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public abstract class RestTestBase {

    protected static InstrumentedSutStarter embeddedStarter;
    protected static String baseUrlOfSut;
    protected static SutController controller;
    protected static RemoteController remoteController;
    protected static int controllerPort;


    private final static int STARTING_SEED = 42;
    protected int defaultSeed = STARTING_SEED;


    @AfterAll
    public static void tearDown() {

        assertTimeoutPreemptively(Duration.ofMinutes(2), () -> {
            boolean stopped = remoteController.stopSUT();
            stopped = embeddedStarter.stop() && stopped;

            assertTrue(stopped);
        });
    }


    @BeforeEach
    public void initTest() {

        //in case it was modified in a previous test in the same class
        defaultSeed = STARTING_SEED;

        assertTimeoutPreemptively(Duration.ofMinutes(2), () -> {
            boolean reset = remoteController.resetSUT();
            assertTrue(reset);
        });
    }


    protected Solution<RestIndividual> initAndRun(List<String> args){
        return (Solution<RestIndividual>) Main.initAndRun(args.toArray(new String[0]));
    }

    protected String outputFolderPath(String outputFolderName){
        return "target/em-tests/" + outputFolderName;
    }


    protected void runAndCheckDeterminism(int iterations, Consumer<List<String>> lambda){

        List<String> args =  new ArrayList<>(Arrays.asList(
                "--createTests", "false",
                "--seed", "42",
                "--showProgress", "false",
                "--avoidNonDeterministicLogs", "true",
                "--sutControllerPort", "" + controllerPort,
                "--maxActionEvaluations", "" + iterations,
                "--stoppingCriterion", "FITNESS_EVALUATIONS"
        ));

        StaticCounter.Companion.reset();
        String firstRun = LoggingUtil.Companion.runWithDeterministicLogger(
                () -> {lambda.accept(args); return Unit.INSTANCE;}
        );

        StaticCounter.Companion.reset();
        String secondRun = LoggingUtil.Companion.runWithDeterministicLogger(
                () -> {lambda.accept(args); return Unit.INSTANCE;}
        );

        assertEquals(firstRun, secondRun);
    }


    protected void runTestHandlingFlaky(
            String outputFolderName,
            String fullClassName,
            int iterations,
            boolean createTests,
            Consumer<List<String>> lambda) throws Throwable{

        runTestHandlingFlaky(outputFolderName, fullClassName, iterations, createTests, lambda, 3);
    }


    protected void runTestHandlingFlaky(
            String outputFolderName,
            String fullClassName,
            int iterations,
            boolean createTests,
            Consumer<List<String>> lambda,
            int timeoutMinutes) throws Throwable{

        runTestHandlingFlaky(outputFolderName, fullClassName, null, iterations, createTests, lambda, timeoutMinutes);
    }

    protected void runTestHandlingFlaky(
            String outputFolderName,
            String fullClassName,
            List<String> terminations,
            int iterations,
            boolean createTests,
            Consumer<List<String>> lambda,
            int timeoutMinutes) throws Throwable{

        List<ClassName> classNames = new ArrayList<>();

        if(terminations == null || terminations.isEmpty()){
            classNames.add(new ClassName(fullClassName));
        } else {
            for (String termination : terminations) {
                classNames.add(new ClassName(fullClassName + termination));
            }
        }

         /*
            Years have passed, still JUnit 5 does not handle global test timeouts :(
            https://github.com/junit-team/junit5/issues/80
         */
        assertTimeoutPreemptively(Duration.ofMinutes(timeoutMinutes), () -> {
            ClassName className = new ClassName(fullClassName);
            clearGeneratedFiles(outputFolderName, classNames);

            handleFlaky(
                    () -> {
                        List<String> args = getArgsWithCompilation(iterations, outputFolderName, className, createTests);
                        defaultSeed++;
                        lambda.accept(new ArrayList<>(args));
                    }
            );
        });
    }



    protected void runTestHandlingFlakyAndCompilation(
            String outputFolderName,
            String fullClassName,
            int iterations,
            Consumer<List<String>> lambda) throws Throwable {

        runTestHandlingFlakyAndCompilation(outputFolderName, fullClassName, Arrays.asList(""), iterations, true, lambda, 3);
    }

    protected void runTestHandlingFlakyAndCompilation(
            String outputFolderName,
            String fullClassName,
            List<String> terminations,
            int iterations,
            Consumer<List<String>> lambda) throws Throwable {

        runTestHandlingFlakyAndCompilation(outputFolderName, fullClassName, terminations, iterations, true, lambda, 3);
    }

    protected void runTestHandlingFlakyAndCompilation(
            String outputFolderName,
            String fullClassName,
            List<String> terminations,
            int iterations,
            boolean createTests,
            Consumer<List<String>> lambda,
            int timeoutMinutes) throws Throwable {

        runTestHandlingFlaky(outputFolderName, fullClassName, terminations, iterations, createTests,lambda, timeoutMinutes);


        //BMR: this is where I should handle multiples???
        if (createTests){
            for (String termination : terminations) {
                assertTimeoutPreemptively(Duration.ofMinutes(2), () -> {
                    ClassName className = new ClassName(fullClassName + termination);
                    clearCompiledFiles(className);
                    //the first one goes through, but for the second generated files appear to not be clean.
                    compileRunAndVerifyTests(outputFolderName, className);
                });
            }
        }
    }

    protected void runTestHandlingFlakyAndCompilation(
            String outputFolderName,
            String fullClassName,
            int iterations,
            boolean createTests,
            Consumer<List<String>> lambda,
            int timeoutMinutes) throws Throwable {

        runTestHandlingFlaky(outputFolderName, fullClassName, iterations, createTests,lambda, timeoutMinutes);

        if (createTests){
            assertTimeoutPreemptively(Duration.ofMinutes(2), () -> {
                ClassName className = new ClassName(fullClassName);
                compileRunAndVerifyTests(outputFolderName, className);
            });
        }
    }

    protected void compileRunAndVerifyTests(String outputFolderName, ClassName className){

        Class<?> klass = loadClass(className);
        assertNull(klass);

        compile(outputFolderName);
        klass = loadClass(className);
        assertNotNull(klass);

        StringWriter writer = new StringWriter();
        PrintWriter pw = new PrintWriter(writer);

        TestExecutionSummary summary = JUnitTestRunner.runTestsInClass(klass);
        summary.printFailuresTo(pw, 100);
        String failures = writer.toString();

        assertTrue(summary.getContainersFoundCount() > 0);
        assertEquals(0, summary.getContainersFailedCount(), failures);
        assertTrue(summary.getContainersSucceededCount() > 0);
        assertTrue(summary.getTestsFoundCount() > 0);
        assertEquals(0, summary.getTestsFailedCount(), failures);
        assertTrue(summary.getTestsSucceededCount() > 0);
    }

    protected void clearGeneratedFiles(String outputFolderName, List<ClassName> testClassNames){

        File folder = new File(outputFolderPath(outputFolderName));
        try{
            FileUtils.deleteDirectory(folder);
        }catch (Exception e){
            throw new RuntimeException(e);
        }

        for (ClassName testClassName : testClassNames){
            clearCompiledFiles(testClassName);
        }

    }

    protected void clearGeneratedFiles(String outputFolderName, ClassName testClassName){
        List<ClassName> classNames = new ArrayList<ClassName>();
        classNames.add(testClassName);

        clearGeneratedFiles(outputFolderName, classNames);
    }

    protected void clearCompiledFiles(ClassName testClassName){
        String byteCodePath = "target/test-classes/" + testClassName.getAsResourcePath();
        File compiledFile = new File(byteCodePath);
        boolean result = compiledFile.delete();

    }

    protected Class<?> loadClass(ClassName className){
        try {
            return this.getClass().getClassLoader().loadClass(className.getFullNameWithDots());
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    protected void compile(String outputFolderName){

        CompilerForTestGenerated.INSTANCE.compile(
                OutputFormat.KOTLIN_JUNIT_5,
                new File(outputFolderPath(outputFolderName)),
                new File("target/test-classes")
        );
    }

        protected List<String> getArgsWithCompilation(int iterations, String outputFolderName, ClassName testClassName){
            return getArgsWithCompilation(iterations, outputFolderName, testClassName, true);
        }

        protected List<String> getArgsWithCompilation(int iterations, String outputFolderName, ClassName testClassName, boolean createTests){

        return new ArrayList<>(Arrays.asList(
                "--createTests", "" + createTests,
                "--seed", "" + defaultSeed,
                "--sutControllerPort", "" + controllerPort,
                "--maxActionEvaluations", "" + iterations,
                "--stoppingCriterion", "FITNESS_EVALUATIONS",
                "--outputFolder", outputFolderPath(outputFolderName),
                "--outputFormat", OutputFormat.KOTLIN_JUNIT_5.toString(),
                "--testSuiteFileName", testClassName.getFullNameWithDots()
        ));
    }

    protected static void initClass(EmbeddedSutController controller) throws Exception {

        RestTestBase.controller = controller;

        embeddedStarter = new InstrumentedSutStarter(controller);
        embeddedStarter.start();

        controllerPort = embeddedStarter.getControllerServerPort();

        remoteController = new RemoteController("localhost", controllerPort, true, true);
        boolean started = remoteController.startSUT();
        assertTrue(started);

        SutInfoDto dto = remoteController.getSutInfo();
        assertNotNull(dto);

        baseUrlOfSut = dto.baseUrlOfSUT;
        assertNotNull(baseUrlOfSut);

        System.out.println("Remote controller running on port " + controllerPort);
        System.out.println("SUT listening on " + baseUrlOfSut);
    }



    protected List<Integer> getIndexOfHttpCalls(Individual ind, HttpVerb verb) {

        List<Integer> indices = new ArrayList<>();
        List<Action> actions = ind.seeActions();

        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i) instanceof RestCallAction) {
                RestCallAction action = (RestCallAction) actions.get(i);
                if (action.getVerb() == verb) {
                    indices.add(i);
                }
            }
        }

        return indices;
    }


    protected boolean hasAtLeastOne(EvaluatedIndividual<RestIndividual> ind,
                                    HttpVerb verb,
                                    int expectedStatusCode) {

        List<Integer> index = getIndexOfHttpCalls(ind.getIndividual(), verb);
        for (int i : index) {
            String statusCode = ind.getResults().get(i).getResultValue(
                    RestCallResult.STATUS_CODE);
            if (statusCode.equals("" + expectedStatusCode)) {
                return true;
            }
        }
        return false;
    }

    protected boolean hasAtLeastOne(EvaluatedIndividual<RestIndividual> ind,
                                    HttpVerb verb,
                                    int expectedStatusCode,
                                    String path,
                                    String inResponse) {

        List<RestAction> actions = ind.getIndividual().seeActions();

        boolean stopped = false;

        for (int i = 0; i < actions.size() && !stopped; i++) {

            RestCallResult res = (RestCallResult) ind.getResults().get(i);
            stopped = res.getStopping();

            if (!(actions.get(i) instanceof RestCallAction)) {
                continue;
            }

            RestCallAction action = (RestCallAction) actions.get(i);

            if (action.getVerb() != verb) {
                continue;
            }

            if (path != null) {
                RestPath target = new RestPath(path);
                if (!action.getPath().isEquivalent(target)) {
                    continue;
                }
            }



            Integer statusCode = res.getStatusCode();

            if (!statusCode.equals(expectedStatusCode)) {
                continue;
            }

            String body = res.getBody();
            if (inResponse != null && (body==null ||  !body.contains(inResponse))) {
                continue;
            }

            return true;
        }

        return false;
    }

    protected void assertHasAtLeastOne(Solution<RestIndividual> solution,
                                       HttpVerb verb,
                                       int expectedStatusCode,
                                       String path,
                                       String inResponse) {

        boolean ok = solution.getIndividuals().stream().anyMatch(
                ind -> hasAtLeastOne(ind, verb, expectedStatusCode, path, inResponse));

        String errorMsg = "Seed " + (defaultSeed-1)+". ";
        errorMsg += "Missing " + expectedStatusCode + " " + verb + " " + path + " " + inResponse + "\n";

        assertTrue(ok, errorMsg + restActions(solution));
    }

    protected void assertInsertionIntoTable(Solution<RestIndividual> solution, String tableName) {

        boolean ok = solution.getIndividuals().stream().anyMatch(
                ind -> ind.getIndividual().getDbInitialization().stream().anyMatch(
                        da -> da.getTable().getName().equalsIgnoreCase(tableName))
        );

        assertTrue(ok);
    }

    protected void assertHasAtLeastOne(Solution<RestIndividual> solution,
                                       HttpVerb verb,
                                       int expectedStatusCode) {
        assertHasAtLeastOne(solution, verb, expectedStatusCode, null, null);
    }

    protected String restActions(Solution<RestIndividual> solution) {
        StringBuffer msg = new StringBuffer("REST calls:\n");

        solution.getIndividuals().stream().flatMap(ind -> ind.evaluatedActions().stream())
                .filter(ea -> ea.getAction() instanceof RestCallAction)
                .map(ea -> {
                    String s = ((RestCallResult)ea.getResult()).getStatusCode() + " ";
                    s += ea.getAction().toString() + "\n";
                    return s;
                })
                .sorted()
                .forEach(s -> msg.append(s));
        ;

        return msg.toString();
    }

    protected void assertNone(Solution<RestIndividual> solution,
                              HttpVerb verb,
                              int expectedStatusCode) {

        boolean ok = solution.getIndividuals().stream().noneMatch(
                ind -> hasAtLeastOne(ind, verb, expectedStatusCode));

        StringBuffer msg = new StringBuffer("REST calls:\n");
        if (!ok) {
            solution.getIndividuals().stream().flatMap(ind -> ind.evaluatedActions().stream())
                    .map(ea -> ea.getAction())
                    .filter(a -> a instanceof RestCallAction)
                    .forEach(a -> msg.append(a.toString() + "\n"));
        }

        assertTrue(ok, msg.toString());
    }

    /**
     * Unfortunately JUnit 5 does not handle flaky tests, and Maven is not upgraded yet.
     * See https://github.com/junit-team/junit5/issues/1558#issuecomment-414701182
     *
     * TODO: once that issue is fixed (if it will ever be fixed), then this method
     * will no longer be needed.
     * Actually no... as we change the seed at each re-execution... so we still need
     * this code
     *
     * @param lambda
     * @throws Throwable
     */
    protected void handleFlaky(Runnable lambda) throws Throwable{

        int attempts = 3;
        Throwable error = null;

        for(int i=0; i<attempts; i++){

            try{
                lambda.run();
                return;
            }catch (OutOfMemoryError e){
                throw e;
            }catch (Throwable t){
                error = t;
            }
        }

        throw error;
    }
}
