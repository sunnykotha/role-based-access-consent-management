package com.example.accessconsent;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BreakGlassServiceTest {

    private final BreakGlassService service = new BreakGlassService();

    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");
    private final User clinician =
            new User(1L, "Emergency Clinician", UserRole.CARDIOLOGIST);
    private final Patient patient =
            new Patient(10L, "Patient A", 45, false, false);

    private AccessRequest request(DataCategory category) {
        return new AccessRequest(
                100L, clinician, patient, category,
                AccessAction.READ, PurposeOfUse.TREATMENT, now);
    }

    @Test
    void createsOneHourFlaggedGrantAndReviewDueWithin24Hours() {
        BreakGlassResult result = service.createGrant(
                request(DataCategory.CLINICAL_NOTES),
                "Emergency treatment in the ED",
                1L,
                1L);

        assertTrue(result.grant().flagged());
        assertEquals(now.plusSeconds(3600), result.grant().expiresAt());
        assertTrue(result.grant().isActiveAt(now));
        assertFalse(result.grant().isActiveAt(now.plusSeconds(3600)));
        assertEquals(now.plusSeconds(24 * 3600L),
                result.reviewTask().dueAt());
    }

    @Test
    void grantIsLimitedToItsCategoryAndTimeWindow() {
        BreakGlassGrant grant = service.createGrant(
                request(DataCategory.CLINICAL_NOTES),
                "Emergency treatment",
                2L,
                2L).grant();

        assertTrue(grant.permits(DataCategory.CLINICAL_NOTES, now));
        assertFalse(grant.permits(DataCategory.FINANCIAL, now));
        assertFalse(grant.permits(
                DataCategory.CLINICAL_NOTES, now.plusSeconds(3600)));
    }

    @Test
    void rejectsMissingEmergencyReason() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createGrant(
                        request(DataCategory.CLINICAL_NOTES),
                        " ",
                        3L,
                        3L));
    }

    @Test
    void psychotherapyNotesRemainUnavailable() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createGrant(
                        request(DataCategory.PSYCHOTHERAPY_NOTES),
                        "Emergency treatment",
                        4L,
                        4L));
    }

    @Test
    void rejectsNonClinicalRoles() {
        User billing = new User(2L, "Billing", UserRole.BILLING_STAFF);
        AccessRequest billingRequest = new AccessRequest(
                101L, billing, patient, DataCategory.CLINICAL_NOTES,
                AccessAction.READ, PurposeOfUse.TREATMENT, now);

        assertThrows(IllegalArgumentException.class,
                () -> service.createGrant(
                        billingRequest, "Emergency", 5L, 5L));
    }

    @Test
    void rejectsEmployeeAndVipRecords() {
        Patient vip = new Patient(11L, "VIP Patient", 40, false, true);
        AccessRequest vipRequest = new AccessRequest(
                102L, clinician, vip, DataCategory.CLINICAL_NOTES,
                AccessAction.READ, PurposeOfUse.TREATMENT, now);

        assertThrows(IllegalArgumentException.class,
                () -> service.createGrant(
                        vipRequest, "Emergency treatment", 6L, 6L));
    }

    @Test
    void rejectsEmployeeAndVipCategoriesDuringBreakGlass() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createGrant(
                        request(DataCategory.EMPLOYEE_RECORD),
                        "Emergency treatment", 7L, 7L));

        assertThrows(IllegalArgumentException.class,
                () -> service.createGrant(
                        request(DataCategory.VIP_RECORD),
                        "Emergency treatment", 8L, 8L));
    }
}