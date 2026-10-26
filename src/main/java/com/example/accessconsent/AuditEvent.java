package com.example.accessconsent;

import java.time.Instant;

public record AuditEvent(
        Long id,
        Long actorUserId,
        Long patientId,
        DataCategory dataCategory,
        AccessAction action,
        PurposeOfUse purpose,
        Instant occurredAt,
        String reason,
        AccessOutcome outcome,
        String policy,
        boolean breakGlass
) {
    public AuditEvent {
        if (id == null || id <= 0
                || actorUserId == null || actorUserId <= 0
                || patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Audit IDs must be positive");
        }
        if (dataCategory == null || action == null || purpose == null
                || occurredAt == null || outcome == null) {
            throw new IllegalArgumentException(
                    "Audit event fields are required");
        }
        if (reason == null || reason.isBlank()
                || policy == null || policy.isBlank()) {
            throw new IllegalArgumentException(
                    "Audit reason and policy are required");
        }
    }
}