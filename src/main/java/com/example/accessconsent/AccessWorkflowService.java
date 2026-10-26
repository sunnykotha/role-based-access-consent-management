package com.example.accessconsent;

import java.util.List;

public class AccessWorkflowService {

    private final AccessPolicyService policyService;
    private final BreakGlassService breakGlassService;
    private final AuditStore auditStore;

    public AccessWorkflowService(
            AccessPolicyService policyService,
            BreakGlassService breakGlassService,
            AuditStore auditStore) {
        if (policyService == null || breakGlassService == null || auditStore == null) {
            throw new IllegalArgumentException("Workflow dependencies are required");
        }
        this.policyService = policyService;
        this.breakGlassService = breakGlassService;
        this.auditStore = auditStore;
    }

    public AccessDecision evaluateAndAudit(
            AccessRequest request,
            List<CareTeamAssignment> assignments,
            List<ConsentDirective> consents,
            long auditEventId,
            String reason) {

        if (request == null || reason == null || reason.isBlank()
                || auditEventId <= 0) {
            throw new IllegalArgumentException(
                    "Request, reason, and positive audit event ID are required");
        }

        AccessDecision decision =
                policyService.evaluate(request, assignments, consents);

        auditStore.append(new AuditEvent(
                auditEventId,
                request.user().id(),
                request.patient().id(),
                request.dataCategory(),
                request.action(),
                request.purpose(),
                request.requestedAt(),
                reason,
                decision.outcome(),
                decision.policy(),
                false));

        return decision;
    }

    public AccessDecision revalidateAndAudit(
            SessionAccessService sessionAccessService,
            AccessRequest originalRequest,
            List<CareTeamAssignment> assignments,
            List<ConsentDirective> consents,
            java.time.Instant checkedAt,
            long auditEventId,
            String reason) {

        if (sessionAccessService == null || originalRequest == null
                || assignments == null || consents == null
                || checkedAt == null || auditEventId <= 0
                || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Session service, request, policy inputs, check time, audit ID, and reason are required");
        }

        AccessDecision decision = sessionAccessService.revalidate(
                originalRequest, assignments, consents, checkedAt);

        auditStore.append(new AuditEvent(
                auditEventId,
                originalRequest.user().id(),
                originalRequest.patient().id(),
                originalRequest.dataCategory(),
                originalRequest.action(),
                originalRequest.purpose(),
                checkedAt,
                reason,
                decision.outcome(),
                decision.policy(),
                false));

        return decision;
    }
    public BreakGlassResult createEmergencyAccessForOutOfTeamAndAudit(
            AccessRequest request,
            List<CareTeamAssignment> assignments,
            String emergencyReason,
            long grantId,
            long reviewTaskId,
            long auditEventId) {

        if (request == null || assignments == null || auditEventId <= 0) {
            throw new IllegalArgumentException(
                    "Request, assignments, and a positive audit event ID are required");
        }

        BreakGlassResult result =
                breakGlassService.createGrantForOutOfTeamClinician(
                        request, assignments, emergencyReason, grantId, reviewTaskId);

        auditStore.append(new AuditEvent(
                auditEventId,
                request.user().id(),
                request.patient().id(),
                request.dataCategory(),
                request.action(),
                request.purpose(),
                request.requestedAt(),
                emergencyReason,
                AccessOutcome.GRANTED,
                "BREAK_GLASS",
                true));

        return result;
    }

    public AccessDecision evaluateBreakGlassAndAudit(
            BreakGlassGrant grant,
            AccessRequest request,
            java.time.Instant checkedAt,
            long auditEventId,
            String accessReason) {

        if (grant == null || request == null || checkedAt == null
                || auditEventId <= 0
                || accessReason == null || accessReason.isBlank()) {
            throw new IllegalArgumentException(
                    "Grant, request, check time, positive audit ID, and reason are required");
        }

        boolean permitted = grant.permitsRequest(request, checkedAt);

        AccessDecision decision = permitted
                ? AccessDecision.allow(
                        "BREAK_GLASS_GRANT",
                        "Active emergency grant permits this request")
                : AccessDecision.deny(
                        "BREAK_GLASS_GRANT_INVALID_OR_EXPIRED",
                        "Emergency grant does not permit this request");

        auditStore.append(new AuditEvent(
                auditEventId,
                request.user().id(),
                request.patient().id(),
                request.dataCategory(),
                request.action(),
                request.purpose(),
                checkedAt,
                accessReason,
                decision.outcome(),
                decision.policy(),
                true));

        return decision;
    }
}