package com.example.accessconsent;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProxyAuthorizationTest {

    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");
    private final Patient minor =
            new Patient(10L, "Minor Patient", 15, false, false);
    private final User guardian =
            new User(2L, "Legal Guardian", UserRole.GUARDIAN);

    private final AccessPolicyService service = new AccessPolicyService();

    private AccessRequest request() {
        return new AccessRequest(
                100L, guardian, minor, DataCategory.CLINICAL_NOTES,
                AccessAction.READ, PurposeOfUse.TREATMENT, now);
    }

    private ProxyAuthorization authorization(Instant revokedAt) {
        return new ProxyAuthorization(
                1L, guardian.id(), minor.id(),
                now.minusSeconds(3600),
                now.plusSeconds(3600),
                revokedAt);
    }

    @Test
    void validGuardianAuthorizationAllowsTreatmentAccess() {
        AccessDecision decision = service.evaluate(
                request(), List.of(), List.of(),
                List.of(authorization(null)));

        assertTrue(decision.allowed());
    }

    @Test
    void guardianWithoutAuthorizationIsDenied() {
        AccessDecision decision = service.evaluate(
                request(), List.of(), List.of(), List.of());

        assertFalse(decision.allowed());
        assertEquals("GUARDIAN_PROXY_VALIDATION_REQUIRED", decision.policy());
    }

    @Test
    void revokedGuardianAuthorizationImmediatelyStopsAccess() {
        ProxyAuthorization revoked = authorization(now);

        AccessDecision decision = service.evaluate(
                request(), List.of(), List.of(), List.of(revoked));

        assertFalse(decision.allowed());
        assertEquals("GUARDIAN_PROXY_VALIDATION_REQUIRED", decision.policy());
    }

    @Test
    void expiredGuardianAuthorizationIsDenied() {
        ProxyAuthorization expired = new ProxyAuthorization(
                2L, guardian.id(), minor.id(),
                now.minusSeconds(7200),
                now,
                null);

        AccessDecision decision = service.evaluate(
                request(), List.of(), List.of(), List.of(expired));

        assertFalse(decision.allowed());
    }

    @Test
    void guardianAuthorizationDoesNotReplaceSensitiveDataConsent() {
        AccessRequest sensitiveRequest = new AccessRequest(
                101L, guardian, minor, DataCategory.HIV_STATUS,
                AccessAction.READ, PurposeOfUse.TREATMENT, now);

        AccessDecision decision = service.evaluate(
                sensitiveRequest, List.of(), List.of(),
                List.of(authorization(null)));

        assertFalse(decision.allowed());
        assertEquals("SENSITIVE_DATA_CONSENT_REQUIRED", decision.policy());
    }
}