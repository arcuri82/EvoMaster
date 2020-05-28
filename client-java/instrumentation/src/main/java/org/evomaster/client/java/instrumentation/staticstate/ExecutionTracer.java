package org.evomaster.client.java.instrumentation.staticstate;

import org.evomaster.client.java.instrumentation.Action;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.TargetInfo;
import org.evomaster.client.java.instrumentation.heuristic.HeuristicsForJumps;
import org.evomaster.client.java.instrumentation.heuristic.Truthness;
import org.evomaster.client.java.instrumentation.shared.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Methods of this class will be injected in the SUT to
 * keep track of what the tests do execute/cover.
 */
public class ExecutionTracer {

    /*
        Careful if you change the signature of any of the
        methods in this class, as they are injected in the
        bytecode instrumentation.
        Fortunately, unit tests should quickly find such
        type of issues.
     */



    /**
     * Key -> the unique descriptive id of the coverage objective
     */
    private static final Map<String, TargetInfo> objectiveCoverage =
            new ConcurrentHashMap<>(65536);

    /**
     * A test case can be composed by 1 or more actions, eg HTTP calls.
     * When we get the best distance for a testing target, we might
     * also want to know which action in the test led to it.
     */
    private static int actionIndex = 0;

    /**
     * A set of possible values used in the tests, needed for some kinds
     * of taint analyses
     */
    private static Set<String> inputVariables = new HashSet<>();

    /**
     * Besides code coverage, there might be other events that we want to
     * keep track during test execution.
     * We keep track of it separately for each action
     */
    private static final List<AdditionalInfo> additionalInfoList = new ArrayList<>();

    static {
        reset();
    }

    public static void reset() {
        objectiveCoverage.clear();
        actionIndex = 0;
        additionalInfoList.clear();
        additionalInfoList.add(new AdditionalInfo());
        inputVariables = new HashSet<>();
    }

    public static void setAction(Action action){
        if(action.getIndex() != actionIndex) {
            actionIndex = action.getIndex();
            additionalInfoList.add(new AdditionalInfo());
        }

        if(action.getInputVariables() != null && !action.getInputVariables().isEmpty()){
            inputVariables = action.getInputVariables();
        }
    }

    /**
     * Check if the given input represented a tainted value from the test cases.
     * This could be based on static info of the input (eg, according to a precise
     * name convention given by TaintInputName), or dynamic info given directly by
     * the test itself (eg, the test at action can register a list of values to check
     * for)
     */
    public static boolean isTaintInput(String input){
        return TaintInputName.isTaintInput(input) || inputVariables.contains(input);
    }


    public static TaintType getTaintType(String input){

        if(input == null){
            return TaintType.NONE;
        }

        if(isTaintInput(input)){
            return TaintType.FULL_MATCH;
        }

        if(TaintInputName.includesTaintInput(input)
                || inputVariables.stream().anyMatch(v -> input.contains(v))){
            return TaintType.PARTIAL_MATCH;
        }

        return TaintType.NONE;
    }

    public static List<AdditionalInfo> exposeAdditionalInfoList() {
        return additionalInfoList;
    }

    public static void markRawAccessOfHttpBodyPayload(){
        additionalInfoList.get(actionIndex).setRawAccessOfHttpBodyPayload(true);
    }

    public static void addParsedDtoName(String name){
        additionalInfoList.get(actionIndex).addParsedDtoName(name);
    }

    public static void addQueryParameter(String param){
        additionalInfoList.get(actionIndex).addQueryParameter(param);
    }

    public static void addHeader(String header){
        additionalInfoList.get(actionIndex).addHeader(header);
    }

    public static void addStringSpecialization(String taintInputName, StringSpecializationInfo info){
        additionalInfoList.get(actionIndex).addSpecialization(taintInputName, info);
    }

    public static void markLastExecutedStatement(String lastLine, String lastMethod){
        additionalInfoList.get(actionIndex).pushLastExecutedStatement(lastLine, lastMethod);
    }

    public static final String COMPLETED_LAST_EXECUTED_STATEMENT_NAME = "completedLastExecutedStatement";
    public static final String COMPLETED_LAST_EXECUTED_STATEMENT_DESCRIPTOR = "()V";

    public static void completedLastExecutedStatement(){
        additionalInfoList.get(actionIndex).popLastExecutedStatement();
    }

    public static Map<String, TargetInfo> getInternalReferenceToObjectiveCoverage() {
        return objectiveCoverage;
    }

    /**
     * @return the number of objectives that have been encountered
     * during the test execution
     */
    public static int getNumberOfObjectives() {
        return objectiveCoverage.size();
    }

    public static int getNumberOfObjectives(String prefix) {
        return (int) objectiveCoverage
                .entrySet().stream()
                .filter(e -> prefix == null || e.getKey().startsWith(prefix))
                .count();
    }

    /**
     * Note: only the objectives encountered so far can have
     * been recorded. So, this is a relative value, not based
     * on the code of the whole SUT (just the parts executed so far).
     * Therefore, it is quite useless for binary values (ie 0 or 1),
     * like current implementation of basic line coverage.
     *
     * @param prefix used for string matching of which objectives types
     *               to consider, eg only lines or only branches.
     *               Use "" or {@code null} to pick up everything
     * @return
     */
    public static int getNumberOfNonCoveredObjectives(String prefix) {

        return getNonCoveredObjectives(prefix).size();
    }

    public static Set<String> getNonCoveredObjectives(String prefix) {

        return objectiveCoverage
                .entrySet().stream()
                .filter(e -> prefix == null || e.getKey().startsWith(prefix))
                .filter(e -> e.getValue().value < 1)
                .map(e -> e.getKey())
                .collect(Collectors.toSet());
    }

    public static Double getValue(String id) {
        return objectiveCoverage.get(id).value;
    }

    private static void updateObjective(String id, double value) {
        if (value < 0d || value > 1d) {
            throw new IllegalArgumentException("Invalid value " + value + " out of range [0,1]");
        }

        /*
            In the same execution, a target could be reached several times,
            so we should keep track of the best value found so far
         */
        if (objectiveCoverage.containsKey(id)) {
            double previous = objectiveCoverage.get(id).value;
            if(value > previous){
                objectiveCoverage.put(id, new TargetInfo(null, id, value, actionIndex));
            }
        } else {
            objectiveCoverage.put(id, new TargetInfo(null, id, value, actionIndex));
        }

        ObjectiveRecorder.update(id, value);
    }

    public static void executedNumericComparison(String idTemplate, double lt, double eq, double gt){

        updateObjective(ObjectiveNaming.numericComparisonObjectiveName(idTemplate, -1), lt);
        updateObjective(ObjectiveNaming.numericComparisonObjectiveName(idTemplate, 0), eq);
        updateObjective(ObjectiveNaming.numericComparisonObjectiveName(idTemplate, +1), gt);
    }

    public static void executedReplacedMethod(String idTemplate, ReplacementType type, Truthness t){

        String idTrue = ObjectiveNaming.methodReplacementObjectiveName(idTemplate, true, type);
        String idFalse = ObjectiveNaming.methodReplacementObjectiveName(idTemplate, false, type);

        updateObjective(idTrue, t.getOfTrue());
        updateObjective(idFalse, t.getOfFalse());
    }


    public static final String EXECUTED_LINE_METHOD_NAME = "executedLine";
    public static final String EXECUTED_LINE_DESCRIPTOR = "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V";

    /**
     * Report on the fact that a given line has been executed.
     */
    public static void executedLine(String className, String methodName, String descriptor, int line) {
        //for targets to cover
        String lineId = ObjectiveNaming.lineObjectiveName(className, line);
        String classId = ObjectiveNaming.classObjectiveName(className);
        updateObjective(lineId, 1d);
        updateObjective(classId, 1d);

        //to calculate last executed line
        String lastLine = className + "_" + line + "_" + methodName;
        String lastMethod = className + "_" + methodName + "_" + descriptor;
        markLastExecutedStatement(lastLine, lastMethod);
    }

    public static final String EXECUTING_METHOD_METHOD_NAME = "executingMethod";
    public static final String EXECUTING_METHOD_DESCRIPTOR = "(Ljava/lang/String;IIZ)V";

    /**
     *  Report on whether method calls have been successfully completed.
     *  Failures can happen due to thrown exceptions.
     *
     * @param className
     * @param line
     * @param index    as there can be many method calls on same line, need to differentiate them
     * @param completed whether the method call was successfully completed.
     */
    public static void executingMethod(String className, int line, int index, boolean completed){
        String id = ObjectiveNaming.successCallObjectiveName(className, line, index);
        if(completed) {
            updateObjective(id, 1d);
        } else {
            updateObjective(id, 0.5);
        }
    }


    //---- branch-jump methods --------------------------

    private static void updateBranch(String className, int line, int branchId, Truthness t) {

        /*
            Note: when we have
            if(x > 0){}

            the "jump" to "else" branch is done if that is false.
            So, the actual evaluated condition is the negation, ie
            x <= 0
         */

        String forThen = ObjectiveNaming.branchObjectiveName(className, line, branchId, true);
        String forElse = ObjectiveNaming.branchObjectiveName(className, line, branchId, false);

        updateObjective(forElse, t.getOfTrue());
        updateObjective(forThen, t.getOfFalse());
    }

    public static final String EXECUTING_BRANCH_JUMP_METHOD_NAME = "executingBranchJump";


    public static final String JUMP_DESC_1_VALUE = "(IILjava/lang/String;II)V";

    public static void executingBranchJump(
            int value, int opcode, String className, int line, int branchId) {

        Truthness t = HeuristicsForJumps.getForSingleValueJump(value, opcode);

        updateBranch(className, line, branchId, t);
    }


    public static final String JUMP_DESC_2_VALUES = "(IIILjava/lang/String;II)V";

    public static void executingBranchJump(
            int firstValue, int secondValue, int opcode, String className, int line, int branchId) {

        Truthness t = HeuristicsForJumps.getForValueComparison(firstValue, secondValue, opcode);

        updateBranch(className, line, branchId, t);
    }

    public static final String JUMP_DESC_OBJECTS =
            "(Ljava/lang/Object;Ljava/lang/Object;ILjava/lang/String;II)V";

    public static void executingBranchJump(
            Object first, Object second, int opcode, String className, int line, int branchId) {

        Truthness t = HeuristicsForJumps.getForObjectComparison(first, second, opcode);

        updateBranch(className, line, branchId, t);
    }


    public static final String JUMP_DESC_NULL =
            "(Ljava/lang/Object;ILjava/lang/String;II)V";

    public static void executingBranchJump(
            Object obj, int opcode, String className, int line, int branchId) {

        Truthness t = HeuristicsForJumps.getForNullComparison(obj, opcode);

        updateBranch(className, line, branchId, t);
    }

}
