package com.example.accessconsent;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BreakGlassAccessWorkflowTest {

    private final Instant grantedAt = Instant.parse("2026-01-01T10:00:00Z");
    private final Instant expiresAt = grantedAt.plusSeconds(3600);

    private final User clinician =
            new User(1L, "Emergency Clinician", UserRole.CARDIOLOGIST);
    private final Patient patient =
            new Patient(10L, "Patient A", 45, false, false);

    private AccessRequest request(Patient targetPatient) {
        return new AccessRequest(
                100L, clinician, targetPatient, DataCategory.CLINICAL_NOTES,
                AccessAction.READ, PurposeOfUse.TREATMENT, grantedAt);
    }

    private BreakGlassGrant grant() {
        return new BreakGlassGrant(
                1L, clinician.id(), patient.id(),
                DataCategory.CLINICAL_NOTES,
                AccessAction.READ, PurposeOfUse.TREATMENT,
                "Emergency treatment in the ED",
                grantedAt, expiresAt);
    }

    private AccessWorkflowService workflow(AuditStore store) {
        return new AccessWorkflowService(
                new AccessPolicyService(),
                new BreakGlassService(),
                store);
    }

    @Test
    void activeMatchingGrantAllowsAndAuditsAccess() {
        AuditStore store = new AuditStore();

        AccessDecision decision = workflow(store).evaluateBreakGlassAndAudit(
                grant(), request(patient), grantedAt.plusSeconds(60),
                1L, "Emergency treatment");

        assertTrue(decision.allowed());
        assertEquals("BREAK_GLASS_GRANT", decision.policy());
        assertEquals(1, store.allEvents().size());

        AuditEvent event = store.allEvents().get(0);
        assertEquals(AccessOutcome.GRANTED, event.outcome());
        assertTrue(event.breakGlass());
        assertEquals("Emergency treatment", event.reason());
    }

    @Test
    void expiredGrantDeniesAndAuditsAccess() {
        AuditStore store = new AuditStore();

        AccessDecision decision = workflow(store).evaluateBreakGlassAndAudit(
                grant(), request(patient), expiresAt,
                2L, "Emergency treatment");

        assertFalse(decision.allowed());
        assertEquals("BREAK_GLASS_GRANT_INVALID_OR_EXPIRED", decision.policy());
        assertEquals(AccessOutcome.DENIED, store.allEvents().get(0).outcome());
        assertTrue(store.allEvents().get(0).breakGlass());
    }

    @Test
    void grantForDifferentPatientDeniesAndAuditsAccess() {
        AuditStore store = new AuditStore();
        Patient otherPatient =
                new Patient(11L, "Patient B", 50, false, false);

        AccessDecision decision = workflow(store).evaluateBreakGlassAndAudit(
                grant(), request(otherPatient), grantedAt.plusSeconds(60),
                3L, "Emergency treatment");

        assertFalse(decision.allowed());
        assertEquals("BREAK_GLASS_GRANT_INVALID_OR_EXPIRED", decision.policy());
        assertEquals(AccessOutcome.DENIED, store.allEvents().get(0).outcome());
        assertEquals(11L, store.allEvents().get(0).patientId());
    }

    @Test
    void directGrantCannotAccessEmployeeOrVipPatient() {
        AuditStore store = new AuditStore();
        Patient employeePatient =
                new Patient(12L, "Employee Patient", 40, true, false);

        AccessDecision decision = workflow(store).evaluateBreakGlassAndAudit(
                grant(), request(employeePatient), grantedAt.plusSeconds(60),
                4L, "Emergency treatment");

        assertFalse(decision.allowed());
        assertEquals(AccessOutcome.DENIED, store.allEvents().get(0).outcome());
        assertTrue(store.allEvents().get(0).breakGlass());
    }

    @Test
    void directlyConstructedGrantCannotAccessEmployeeRecordCategory() {
        AuditStore store = new AuditStore();
        BreakGlassGrant employeeRecordGrant = new BreakGlassGrant(
                2L, clinician.id(), patient.id(),
                DataCategory.EMPLOYEE_RECORD,
                AccessAction.READ, PurposeOfUse.TREATMENT,
                "Emergency treatment",
                grantedAt, expiresAt);
        AccessRequest employeeRecordRequest = new AccessRequest(
                101L, clinician, patient, DataCategory.EMPLOYEE_RECORD,
                AccessAction.READ, PurposeOfUse.TREATMENT, grantedAt);

        AccessDecision decision = workflow(store).evaluateBreakGlassAndAudit(
                employeeRecordGrant, employeeRecordRequest,
                grantedAt.plusSeconds(60), 5L, "Emergency treatment");

        assertFalse(decision.allowed());
        assertEquals(AccessOutcome.DENIED, store.allEvents().get(0).outcome());
    }
}