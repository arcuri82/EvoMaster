package org.evomaster.client.java.controller.api.dto;

/**
 * Created by arcuri82 on 14-Jun-19.
 */
public class HeuristicEntryDto {

    /**
     * The type of extra heuristic.
     * Note: for the moment, we only have heuristics on SQL commands
     */
    public enum Type {SQL}

    /**
     * Should we try to minimize or maximize the heuristic?
     */
    public enum Objective {
        /**
         * The lower the better.
         * Minimum is 0. It can be considered as a "distance" to minimize.
         */
        MINIMIZE_TO_ZERO,
        /**
         * The higher the better.
         * Note: given x, we could rather considered the value
         * 1/x to minimize. But that wouldn't work for negative x,
         * and also would make debugging more difficult (ie better to
         * look at the raw, non-transformed values).
         */
        MAXIMIZE
    }


    public Type type;

    public Objective objective;

    /**
     * An id representing this heuristics.
     * For example, for SQL, it could be a SQL command
     */
    public String id;


    /**
     * The actual value of the heuristic
     */
    public Double value;


    public HeuristicEntryDto() {
    }

    public HeuristicEntryDto(Type type, Objective objective, String id, Double value) {
        this.type = type;
        this.objective = objective;
        this.id = id;
        this.value = value;
    }
}
