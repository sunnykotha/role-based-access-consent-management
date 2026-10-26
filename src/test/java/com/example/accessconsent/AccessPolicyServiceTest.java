package com.example.accessconsent;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AccessPolicyServiceTest {

    private final AccessPolicyService service = new AccessPolicyService();

    private final User clinician =
            new User(1L, "Dr. Lee", UserRole.CLINICIAN);
    private final Patient patient =
            new Patient(10L, "Patient A", 45, false, false);
    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");

    private AccessRequest request(DataCategory category, PurposeOfUse purpose) {
        return new AccessRequest(
                100L, clinician, patient, category,
                AccessAction.READ, purpose, now);
    }

    private CareTeamAssignment activeAssignment() {
        return new CareTeamAssignment(
                clinician.id(), patient.id(),
                now.minusSeconds(3600), now.plusSeconds(3600));
    }

    @Test
    void grantsClinicalAccessToActiveCareTeamForTreatment() {
        AccessDecision decision = service.evaluate(
                request(DataCategory.CLINICAL_NOTES, PurposeOfUse.TREATMENT),
                List.of(activeAssignment()), List.of());

        assertTrue(decision.allowed());
        assertEquals("CARE_TEAM_TREATMENT", decision.policy());
    }

    @Test
    void deniesClinicalAccessWithoutActiveCareTeamAssignment() {
        AccessDecision decision = service.evaluate(
                request(DataCategory.CLINICAL_NOTES, PurposeOfUse.TREATMENT),
                List.of(), List.of());

        assertFalse(decision.allowed());
        assertEquals("ACTIVE_CARE_TEAM_REQUIRED", decision.policy());
    }

    @Test
    void deniesClinicalAccessForPaymentPurpose() {
        AccessDecision decision = service.evaluate(
                request(DataCategory.CLINICAL_NOTES, PurposeOfUse.PAYMENT),
                List.of(activeAssignment()), List.of());

        assertFalse(decision.allowed());
        assertEquals("CLINICAL_PURPOSE_REQUIRED", decision.policy());
    }

    @Test
    void deniesSensitiveDataWithoutConsent() {
        AccessDecision decision = service.evaluate(
                request(DataCategory.HIV_STATUS, PurposeOfUse.TREATMENT),
                List.of(activeAssignment()), List.of());

        assertFalse(decision.allowed());
        assertEquals("SENSITIVE_DATA_CONSENT_REQUIRED", decision.policy());
    }

    @Test
    void billingStaffCanReadFinancialDataForPayment() {
        User billing = new User(2L, "Billing", UserRole.BILLING_STAFF);
        AccessRequest billingRequest = new AccessRequest(
                101L, billing, patient, DataCategory.FINANCIAL,
                AccessAction.READ, PurposeOfUse.PAYMENT, now);

        AccessDecision decision = service.evaluate(
                billingRequest, List.of(), List.of());

        assertTrue(decision.allowed());
        assertEquals("BILLING_MINIMUM_NECESSARY", decision.policy());
    }

    @Test
    void billingStaffCannotReadClinicalNotes() {
        User billing = new User(2L, "Billing", UserRole.BILLING_STAFF);
        AccessRequest billingRequest = new AccessRequest(
                102L, billing, patient, DataCategory.CLINICAL_NOTES,
                AccessAction.READ, PurposeOfUse.PAYMENT, now);

        AccessDecision decision = service.evaluate(
                billingRequest, List.of(), List.of());

        assertFalse(decision.allowed());
    }

    @Test
    void psychotherapyNotesAreDeniedThroughOrdinaryAccess() {
        AccessDecision decision = service.evaluate(
                request(DataCategory.PSYCHOTHERAPY_NOTES, PurposeOfUse.TREATMENT),
                List.of(activeAssignment()), List.of());

        assertFalse(decision.allowed());
        assertEquals("PSYCHOTHERAPY_NOTES_RESTRICTED", decision.policy());
    }

    @Test
    void employeeAndVipRecordsRequirePrivilegedRole() {
        Patient vip = new Patient(11L, "VIP Patient", 40, false, true);
        AccessRequest vipRequest = new AccessRequest(
                103L, clinician, vip, DataCategory.CLINICAL_NOTES,
                AccessAction.READ, PurposeOfUse.TREATMENT, now);

        AccessDecision decision = service.evaluate(
                vipRequest, List.of(), List.of());

        assertFalse(decision.allowed());
        assertEquals("EMPLOYEE_VIP_PROTECTION", decision.policy());
    }

    @Test
    void rejectsNullPolicyInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> service.evaluate(null, List.of(), List.of()));
    }

    @Test
    void privacyOfficerCanAccessSensitiveDataUnderPrivilegedRolePolicy() {
        User officer = new User(3L, "Privacy Officer", UserRole.PRIVACY_OFFICER);
        AccessRequest sensitiveRequest = new AccessRequest(
                104L, officer, patient, DataCategory.HIV_STATUS,
                AccessAction.READ, PurposeOfUse.TREATMENT, now);

        AccessDecision decision = service.evaluate(
                sensitiveRequest, List.of(), List.of());

        assertTrue(decision.allowed());
        assertEquals("PRIVACY_ADMINISTRATIVE_ACCESS", decision.policy());
    }

    @Test
    void administratorCanAccessSensitiveDataUnderPrivilegedRolePolicy() {
        User administrator =
                new User(4L, "Administrator", UserRole.ADMINISTRATOR);
        AccessRequest sensitiveRequest = new AccessRequest(
                105L, administrator, patient, DataCategory.MENTAL_HEALTH,
                AccessAction.READ, PurposeOfUse.TREATMENT, now);

        AccessDecision decision = service.evaluate(
                sensitiveRequest, List.of(), List.of());

        assertTrue(decision.allowed());
        assertEquals("PRIVACY_ADMINISTRATIVE_ACCESS", decision.policy());
    }

    @Test
    void deniesEmployeeAndVipRecordCategoriesWithoutPatientFlags() {
        AccessDecision employeeDecision = service.evaluate(
                request(DataCategory.EMPLOYEE_RECORD, PurposeOfUse.TREATMENT),
                List.of(), List.of());

        AccessDecision vipDecision = service.evaluate(
                request(DataCategory.VIP_RECORD, PurposeOfUse.TREATMENT),
                List.of(), List.of());

        assertFalse(employeeDecision.allowed());
        assertEquals("EMPLOYEE_VIP_PROTECTION", employeeDecision.policy());
        assertFalse(vipDecision.allowed());
        assertEquals("EMPLOYEE_VIP_PROTECTION", vipDecision.policy());
    }
}