package org.evomaster.client.java.controller.api.dto;

public class SutRunDto {

    /**
     * Whether the SUT should be running
     */
    public Boolean run;

    /**
     * Whether the internal state of the SUT should be reset
     */
    public Boolean resetState;

    /**
     * Whether SQL heuristics should be computed.
     * Note: those can be very expensive
     */
    public Boolean calculateSqlHeuristics;

    /**
     * Whether SQL execution info should be saved.
     */
    public Boolean extractSqlExecutionInfo;


    /**
     * Whether Mongo execution info should be saved.
     */
    public Boolean extractMongoExecutionInfo;


    public SutRunDto() {
    }

    public SutRunDto(Boolean run,
                     Boolean resetState,
                     Boolean calculateSqlHeuristics,
                     Boolean extractSqlExecutionInfo,
                     Boolean extractMongoExecutionInfo) {
        if (calculateSqlHeuristics != null && calculateSqlHeuristics && extractSqlExecutionInfo != null && !extractSqlExecutionInfo)
            throw new IllegalArgumentException("extractSqlExecutionInfo should be enabled when calculateSqlHeuristics is enabled");

        this.run = run;
        this.resetState = resetState;
        this.calculateSqlHeuristics = calculateSqlHeuristics;
        this.extractSqlExecutionInfo = extractSqlExecutionInfo;
        this.extractMongoExecutionInfo = extractMongoExecutionInfo;
    }

    public SutRunDto(Boolean run, Boolean resetState, Boolean calculateSqlHeuristics, Boolean extractMongoExecutionInfo) {
        this(run,
                resetState,
                calculateSqlHeuristics,
                calculateSqlHeuristics != null && calculateSqlHeuristics,
                extractMongoExecutionInfo != null && extractMongoExecutionInfo);
    }
}
