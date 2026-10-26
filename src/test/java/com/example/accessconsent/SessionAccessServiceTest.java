package com.example.accessconsent;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SessionAccessServiceTest {

    private final SessionAccessService service =
            new SessionAccessService(new AccessPolicyService());

    private final Instant startedAt =
            Instant.parse("2026-01-01T10:00:00Z");
    private final Instant later =
            Instant.parse("2026-01-01T10:30:00Z");

    private final User clinician =
            new User(1L, "Clinician", UserRole.CLINICIAN);
    private final Patient patient =
            new Patient(10L, "Patient A", 45, false, false);

    private AccessRequest request(DataCategory category) {
        return new AccessRequest(
                100L, clinician, patient, category,
                AccessAction.READ, PurposeOfUse.TREATMENT, startedAt);
    }

    private CareTeamAssignment assignment(Instant end) {
        return new CareTeamAssignment(
                clinician.id(), patient.id(),
                startedAt.minusSeconds(60), end);
    }

    @Test
    void accessIsDeniedAfterCareTeamAssignmentExpires() {
        AccessRequest original = request(DataCategory.CLINICAL_NOTES);
        CareTeamAssignment expiring = assignment(later);

        assertTrue(service.revalidate(
                original, List.of(expiring), List.of(), startedAt).allowed());

        AccessDecision decision = service.revalidate(
                original, List.of(expiring), List.of(), later);

        assertFalse(decision.allowed());
        assertEquals("ACTIVE_CARE_TEAM_REQUIRED", decision.policy());
    }

    @Test
    void revokedSensitiveDataConsentBlocksSubsequentSessionAccess() {
        AccessRequest original = request(DataCategory.HIV_STATUS);

        ConsentDirective consent = new ConsentDirective(
                1L,
                patient.id(),
                Set.of(DataCategory.HIV_STATUS),
                Set.of(PurposeOfUse.TREATMENT),
                startedAt.minusSeconds(60),
                later.plusSeconds(3600),
                later);

        CareTeamAssignment active = assignment(later.plusSeconds(3600));

        assertTrue(service.revalidate(
                original, List.of(active), List.of(consent), startedAt).allowed());

        AccessDecision afterRevocation = service.revalidate(
                original, List.of(active), List.of(consent), later);

        assertFalse(afterRevocation.allowed());
        assertEquals("SENSITIVE_DATA_CONSENT_REQUIRED",
                afterRevocation.policy());
    }

    @Test
    void activeConsentAndAssignmentContinueToAllowAccess() {
        AccessRequest original = request(DataCategory.HIV_STATUS);

        ConsentDirective consent = new ConsentDirective(
                2L,
                patient.id(),
                Set.of(DataCategory.HIV_STATUS),
                Set.of(PurposeOfUse.TREATMENT),
                startedAt.minusSeconds(60),
                later.plusSeconds(3600),
                null);

        CareTeamAssignment active = assignment(later.plusSeconds(3600));

        AccessDecision decision = service.revalidate(
                original, List.of(active), List.of(consent), later);

        assertTrue(decision.allowed());
    }

    @Test
    void rejectsMissingRevalidationInputs() {
        AccessRequest original = request(DataCategory.CLINICAL_NOTES);

        assertThrows(IllegalArgumentException.class,
                () -> service.revalidate(
                        null, List.of(), List.of(), later));
        assertThrows(IllegalArgumentException.class,
                () -> service.revalidate(
                        original, List.of(), List.of(), null));
    }
}