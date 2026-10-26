package com.example.accessconsent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class SessionRevalidationAuditTest {

    private final Instant start = Instant.parse("2026-01-01T10:00:00Z");
    private final Instant later = start.plusSeconds(1800);
    private final User clinician =
            new User(1L, "Clinician", UserRole.CLINICIAN);
    private final Patient patient =
            new Patient(10L, "Patient", 40, false, false);
    private final AccessRequest request = new AccessRequest(
            100L, clinician, patient,
            DataCategory.CLINICAL_NOTES,
            AccessAction.READ,
            PurposeOfUse.TREATMENT,
            start);

    private AccessWorkflowService workflow(AuditStore store) {
        return new AccessWorkflowService(
                new AccessPolicyService(),
                new BreakGlassService(),
                store);
    }

    @Test
    void auditsGrantedSessionRevalidation() {
        AuditStore store = new AuditStore();
        CareTeamAssignment assignment = new CareTeamAssignment(
                clinician.id(), patient.id(),
                start.minusSeconds(60), later.plusSeconds(3600));

        AccessDecision decision = workflow(store).revalidateAndAudit(
                new SessionAccessService(new AccessPolicyService()),
                request, List.of(assignment), List.of(),
                later, 501L, "Session access revalidation");

        assertTrue(decision.allowed());
        assertEquals(1, store.allEvents().size());
        assertEquals(AccessOutcome.GRANTED, store.allEvents().get(0).outcome());
        assertEquals(later, store.allEvents().get(0).occurredAt());
    }

    @Test
    void auditsDeniedSessionRevalidationAfterAssignmentExpires() {
        AuditStore store = new AuditStore();
        CareTeamAssignment assignment = new CareTeamAssignment(
                clinician.id(), patient.id(),
                start.minusSeconds(60), later);

        AccessDecision decision = workflow(store).revalidateAndAudit(
                new SessionAccessService(new AccessPolicyService()),
                request, List.of(assignment), List.of(),
                later, 502L, "Session access revalidation");

        assertFalse(decision.allowed());
        assertEquals(1, store.allEvents().size());
        assertEquals(AccessOutcome.DENIED, store.allEvents().get(0).outcome());
        assertEquals(later, store.allEvents().get(0).occurredAt());
    }
}