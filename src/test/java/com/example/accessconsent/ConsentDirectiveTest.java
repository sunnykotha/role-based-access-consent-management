package com.example.accessconsent;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ConsentDirectiveTest {
    private final Instant start = Instant.parse("2026-01-01T00:00:00Z");
    private final Instant end = Instant.parse("2026-03-01T00:00:00Z");

    private ConsentDirective consent(Instant revokedAt) {
        return new ConsentDirective(
                1L, 2L,
                Set.of(DataCategory.MENTAL_HEALTH),
                Set.of(PurposeOfUse.TREATMENT),
                start, end, revokedAt);
    }

    @Test
    void consentPermitsOnlyItsCategoryAndPurpose() {
        var consent = consent(null);
        var now = Instant.parse("2026-02-01T00:00:00Z");

        assertTrue(consent.permits(2L, DataCategory.MENTAL_HEALTH, PurposeOfUse.TREATMENT, now));
        assertFalse(consent.permits(2L, DataCategory.HIV_STATUS, PurposeOfUse.TREATMENT, now));
        assertFalse(consent.permits(2L, DataCategory.MENTAL_HEALTH, PurposeOfUse.PAYMENT, now));
    }

    @Test
    void expiredConsentDoesNotPermitAccess() {
        assertFalse(consent(null).permits(
                2L, DataCategory.MENTAL_HEALTH, PurposeOfUse.TREATMENT, end));
    }

    @Test
    void revocationImmediatelyStopsConsentBasedPermission() {
        var revokedAt = Instant.parse("2026-02-10T12:00:00Z");
        var consent = consent(revokedAt);

        assertTrue(consent.permits(2L, DataCategory.MENTAL_HEALTH,
                PurposeOfUse.TREATMENT, revokedAt.minusSeconds(1)));
        assertFalse(consent.permits(2L, DataCategory.MENTAL_HEALTH,
                PurposeOfUse.TREATMENT, revokedAt));
        assertFalse(consent.permits(2L, DataCategory.MENTAL_HEALTH,
                PurposeOfUse.TREATMENT, revokedAt.plusSeconds(1)));
    }
}