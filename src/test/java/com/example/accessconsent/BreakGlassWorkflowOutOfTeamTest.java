package com.example.accessconsent;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BreakGlassWorkflowOutOfTeamTest {

    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");
    private final User clinician =
            new User(1L, "Emergency Cardiologist", UserRole.CARDIOLOGIST);
    private final Patient patient =
            new Patient(10L, "Patient A", 45, false, false);

    private AccessRequest request() {
        return new AccessRequest(
                100L,
                clinician,
                patient,
                DataCategory.CLINICAL_NOTES,
                AccessAction.READ,
                PurposeOfUse.TREATMENT,
                now);
    }

    private AccessWorkflowService workflow(AuditStore auditStore) {
        return new AccessWorkflowService(
                new AccessPolicyService(),
                new BreakGlassService(),
                auditStore);
    }

    @Test
    void rejectsEmergencyGrantWhenClinicianIsOnActiveCareTeam() {
        AuditStore auditStore = new AuditStore();
        CareTeamAssignment activeAssignment = new CareTeamAssignment(
                clinician.id(),
                patient.id(),
                now.minusSeconds(60),
                now.plusSeconds(3600));

        assertThrows(IllegalArgumentException.class,
                () -> workflow(auditStore).createEmergencyAccessForOutOfTeamAndAudit(
                        request(),
                        List.of(activeAssignment),
                        "Emergency treatment",
                        1L,
                        1L,
                        1L));

        assertTrue(auditStore.allEvents().isEmpty());
    }

    @Test
    void grantsAndAuditsEmergencyAccessForOutOfTeamClinician() {
        AuditStore auditStore = new AuditStore();

        BreakGlassResult result =
                workflow(auditStore).createEmergencyAccessForOutOfTeamAndAudit(
                        request(),
                        List.of(),
                        "Emergency treatment in the ED",
                        2L,
                        2L,
                        2L);

        assertTrue(result.grant().flagged());
        assertTrue(result.grant().isActiveAt(now));
        assertEquals(now.plusSeconds(3600), result.grant().expiresAt());
        assertEquals(now.plusSeconds(24 * 3600L),
                result.reviewTask().dueAt());

        assertEquals(1, auditStore.allEvents().size());
        AuditEvent event = auditStore.allEvents().get(0);
        assertEquals(AccessOutcome.GRANTED, event.outcome());
        assertEquals("BREAK_GLASS", event.policy());
        assertTrue(event.breakGlass());
        assertEquals("Emergency treatment in the ED", event.reason());
    }
}