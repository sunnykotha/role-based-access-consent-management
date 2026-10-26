package com.example.accessconsent;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AccessWorkflowServiceTest {

    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");
    private final User cardiologist =
            new User(1L, "Emergency Cardiologist", UserRole.CARDIOLOGIST);
    private final Patient patient =
            new Patient(10L, "Patient A", 45, false, false);

    private AccessRequest request(DataCategory category) {
        return new AccessRequest(
                100L, cardiologist, patient, category,
                AccessAction.READ, PurposeOfUse.TREATMENT, now);
    }

    private AccessWorkflowService workflow(AuditStore auditStore) {
        return new AccessWorkflowService(
                new AccessPolicyService(),
                new BreakGlassService(),
                auditStore);
    }

    @Test
    void deniedOrdinaryAccessIsAudited() {
        AuditStore auditStore = new AuditStore();

        AccessDecision decision = workflow(auditStore).evaluateAndAudit(
                request(DataCategory.CLINICAL_NOTES),
                List.of(),
                List.of(),
                1L,
                "Routine chart access");

        assertFalse(decision.allowed());
        assertEquals(1, auditStore.allEvents().size());
        assertEquals(AccessOutcome.DENIED,
                auditStore.allEvents().get(0).outcome());
        assertEquals("ACTIVE_CARE_TEAM_REQUIRED",
                auditStore.allEvents().get(0).policy());
    }

    @Test
    void emergencyDepartmentExampleCreatesGrantReviewAndAuditEvent() {
        AuditStore auditStore = new AuditStore();
        AccessWorkflowService workflow = workflow(auditStore);

        BreakGlassResult result = workflow.createEmergencyAccessForOutOfTeamAndAudit(
                request(DataCategory.CLINICAL_NOTES),
                List.of(),
                "Emergency treatment in the ED",
                1L,
                1L,
                1L);

        assertTrue(result.grant().flagged());
        assertTrue(result.grant().isActiveAt(now));
        assertEquals(now.plusSeconds(3600), result.grant().expiresAt());
        assertEquals(now.plusSeconds(24 * 3600L),
                result.reviewTask().dueAt());

        AuditEvent event = auditStore.allEvents().get(0);
        assertEquals(1L, event.actorUserId());
        assertEquals(10L, event.patientId());
        assertEquals(AccessOutcome.GRANTED, event.outcome());
        assertTrue(event.breakGlass());
        assertEquals("BREAK_GLASS", event.policy());
    }

    @Test
    void psychotherapyNotesRemainMaskedDuringEmergencyAccess() {
        AuditStore auditStore = new AuditStore();
        AccessWorkflowService workflow = workflow(auditStore);

        assertThrows(IllegalArgumentException.class,
                () -> workflow.createEmergencyAccessForOutOfTeamAndAudit(
                        request(DataCategory.PSYCHOTHERAPY_NOTES),
                        List.of(),
                        "Emergency treatment",
                        2L,
                        2L,
                        2L));

        PatientData data = new PatientData(10L, Map.of(
                DataCategory.CLINICAL_NOTES, "Clinical notes",
                DataCategory.PSYCHOTHERAPY_NOTES, "Private psychotherapy notes"));

        Map<DataCategory, String> visible =
                new FieldMaskingService().visibleFields(cardiologist, data);

        assertTrue(visible.containsKey(DataCategory.CLINICAL_NOTES));
        assertFalse(visible.containsKey(DataCategory.PSYCHOTHERAPY_NOTES));
        assertTrue(auditStore.allEvents().isEmpty());
    }

    @Test
    void ordinaryGrantedAccessIsAudited() {
        AuditStore auditStore = new AuditStore();
        CareTeamAssignment assignment = new CareTeamAssignment(
                cardiologist.id(),
                patient.id(),
                now.minusSeconds(60),
                now.plusSeconds(3600));

        AccessDecision decision = workflow(auditStore).evaluateAndAudit(
                request(DataCategory.CLINICAL_NOTES),
                List.of(assignment),
                List.of(),
                3L,
                "Treatment");

        assertTrue(decision.allowed());
        assertEquals(AccessOutcome.GRANTED,
                auditStore.allEvents().get(0).outcome());
        assertFalse(auditStore.allEvents().get(0).breakGlass());
    }
}