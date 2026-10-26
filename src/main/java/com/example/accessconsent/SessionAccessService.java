package com.example.accessconsent;

import java.time.Instant;
import java.util.List;

public class SessionAccessService {

    private final AccessPolicyService policyService;

    public SessionAccessService(AccessPolicyService policyService) {
        if (policyService == null) {
            throw new IllegalArgumentException("Policy service is required");
        }
        this.policyService = policyService;
    }

    public AccessDecision revalidate(
            AccessRequest originalRequest,
            List<CareTeamAssignment> assignments,
            List<ConsentDirective> consents,
            Instant checkedAt) {

        if (originalRequest == null || assignments == null
                || consents == null || checkedAt == null) {
            throw new IllegalArgumentException(
                    "Request, assignments, consents, and check time are required");
        }

        AccessRequest currentRequest = new AccessRequest(
                originalRequest.id(),
                originalRequest.user(),
                originalRequest.patient(),
                originalRequest.dataCategory(),
                originalRequest.action(),
                originalRequest.purpose(),
                checkedAt);

        return policyService.evaluate(currentRequest, assignments, consents);
    }
}