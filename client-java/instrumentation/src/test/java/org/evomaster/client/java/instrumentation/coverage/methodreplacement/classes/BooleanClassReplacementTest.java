package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper;
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BooleanClassReplacementTest {

    @BeforeEach
    public void setUp() {
        ExecutionTracer.reset();
    }

    @Test
    public void testTrue() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean booleanValue = BooleanClassReplacement.parseBoolean("true", prefix);
        assertTrue(booleanValue);
        int numberOfNonCoveredObjectives = ExecutionTracer.getNumberOfNonCoveredObjectives(prefix);
        assertEquals(1, numberOfNonCoveredObjectives);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        String objectiveId = nonCoveredObjectives.iterator().next();
        double value = ExecutionTracer.getValue(objectiveId);
        assertEquals(0, value);
    }

    @Test
    public void testFalse() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean booleanValue0 = BooleanClassReplacement.parseBoolean("false", prefix);
        assertFalse(booleanValue0);
        int numberOfNonCoveredObjectives = ExecutionTracer.getNumberOfNonCoveredObjectives(prefix);
        assertEquals(1, numberOfNonCoveredObjectives);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        String objectiveId = nonCoveredObjectives.iterator().next();
        double heuristicValue0 = ExecutionTracer.getValue(objectiveId);
        assertTrue(heuristicValue0 > DistanceHelper.H_NOT_NULL);

        boolean booleanValue1 = BooleanClassReplacement.parseBoolean("tr_e", prefix);
        assertFalse(booleanValue1);
        double heuristicValue1 = ExecutionTracer.getValue(objectiveId);
        assertTrue(heuristicValue1 > 0);
        assertTrue(heuristicValue1 > heuristicValue0);

        boolean booleanValue2 = BooleanClassReplacement.parseBoolean("trUe", prefix);
        assertTrue(booleanValue2);
        double heuristicValue2 = ExecutionTracer.getValue(objectiveId);
        assertEquals(1, heuristicValue2);
    }

    @Test
    public void testNull() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean booleanValue = BooleanClassReplacement.parseBoolean(null, prefix);
        assertFalse(booleanValue);
        Set<String> nonCoveredObjectives = ExecutionTracer.getNonCoveredObjectives(prefix);
        assertEquals(1, nonCoveredObjectives.size());
        String objectiveId = nonCoveredObjectives.iterator().next();
        double value = ExecutionTracer.getValue(objectiveId);
        assertEquals(DistanceHelper.H_REACHED_BUT_NULL, value);
    }

    @Test
    public void testValueOf() {
        String prefix = ObjectiveNaming.METHOD_REPLACEMENT + "IdTemplate";
        boolean booleanValue = BooleanClassReplacement.valueOf("true", prefix);
        assertTrue(booleanValue);
    }

}
