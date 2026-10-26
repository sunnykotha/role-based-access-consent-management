package com.example.accessconsent;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CareTeamAssignmentTest {
    private final Instant start = Instant.parse("2026-01-01T00:00:00Z");
    private final Instant end = Instant.parse("2026-02-01T00:00:00Z");

    @Test
    void activeWithinAssignmentPeriod() {
        var assignment = new CareTeamAssignment(1L, 2L, start, end);
        assertTrue(assignment.covers(1L, 2L, Instant.parse("2026-01-15T00:00:00Z")));
    }

    @Test
    void expiredAssignmentDoesNotGrantMembership() {
        var assignment = new CareTeamAssignment(1L, 2L, start, end);
        assertFalse(assignment.covers(1L, 2L, end));
    }

    @Test
    void assignmentDoesNotMatchAnotherPatient() {
        var assignment = new CareTeamAssignment(1L, 2L, start, null);
        assertFalse(assignment.covers(1L, 3L, start));
    }
}