package com.example.accessconsent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class BreakGlassGrantScopeTest {

    private final Instant grantedAt =
            Instant.parse("2026-01-01T10:00:00Z");

    private final User clinician =
            new User(1L, "Emergency Clinician", UserRole.CLINICIAN);

    private final Patient patient =
            new Patient(10L, "Test Patient", 40, false, false);

    private BreakGlassGrant grant() {
        return new BreakGlassGrant(
                100L,
                clinician.id(),
                patient.id(),
                DataCategory.CLINICAL_NOTES,
                AccessAction.READ,
                PurposeOfUse.TREATMENT,
                "Emergency treatment",
                grantedAt,
                grantedAt.plusSeconds(3600));
    }

    private AccessRequest request(
            User user,
            Patient requestedPatient,
            DataCategory category,
            AccessAction action,
            PurposeOfUse purpose,
            Instant requestedAt) {
        return new AccessRequest(
                200L,
                user,
                requestedPatient,
                category,
                action,
                purpose,
                requestedAt);
    }

    @Test
    void permitsMatchingRequestDuringGrantWindow() {
        AccessRequest request = request(
                clinician, patient,
                DataCategory.CLINICAL_NOTES,
                AccessAction.READ,
                PurposeOfUse.TREATMENT,
                grantedAt.plusSeconds(60));

        assertTrue(grant().permitsRequest(request, grantedAt.plusSeconds(60)));
    }

    @Test
    void rejectsDifferentUser() {
        User other = new User(2L, "Other Clinician", UserRole.CLINICIAN);
        AccessRequest request = request(
                other, patient,
                DataCategory.CLINICAL_NOTES,
                AccessAction.READ,
                PurposeOfUse.TREATMENT,
                grantedAt.plusSeconds(60));

        assertFalse(grant().permitsRequest(request, grantedAt.plusSeconds(60)));
    }

    @Test
    void rejectsDifferentPatient() {
        Patient other = new Patient(11L, "Other Patient", 35, false, false);
        AccessRequest request = request(
                clinician, other,
                DataCategory.CLINICAL_NOTES,
                AccessAction.READ,
                PurposeOfUse.TREATMENT,
                grantedAt.plusSeconds(60));

        assertFalse(grant().permitsRequest(request, grantedAt.plusSeconds(60)));
    }

    @Test
    void rejectsDifferentCategoryActionOrPurpose() {
        Instant checkedAt = grantedAt.plusSeconds(60);

        assertFalse(grant().permitsRequest(request(
                clinician, patient,
                DataCategory.FINANCIAL,
                AccessAction.READ,
                PurposeOfUse.TREATMENT,
                checkedAt), checkedAt));

        assertFalse(grant().permitsRequest(request(
                clinician, patient,
                DataCategory.CLINICAL_NOTES,
                AccessAction.WRITE,
                PurposeOfUse.TREATMENT,
                checkedAt), checkedAt));

        assertFalse(grant().permitsRequest(request(
                clinician, patient,
                DataCategory.CLINICAL_NOTES,
                AccessAction.READ,
                PurposeOfUse.OPERATIONS,
                checkedAt), checkedAt));
    }

    @Test
    void rejectsExpiredGrantAndPsychotherapyNotes() {
        Instant expiresAt = grantedAt.plusSeconds(3600);

        AccessRequest originalRequest = request(
                clinician, patient,
                DataCategory.CLINICAL_NOTES,
                AccessAction.READ,
                PurposeOfUse.TREATMENT,
                expiresAt);

        assertFalse(grant().permitsRequest(originalRequest, expiresAt));

        AccessRequest psychotherapyRequest = request(
                clinician, patient,
                DataCategory.PSYCHOTHERAPY_NOTES,
                AccessAction.READ,
                PurposeOfUse.TREATMENT,
                grantedAt.plusSeconds(60));

        assertFalse(grant().permitsRequest(
                psychotherapyRequest, grantedAt.plusSeconds(60)));
    }

    @Test
    void rejectsNullRequest() {
        assertFalse(grant().permitsRequest(null, grantedAt));
    }
}