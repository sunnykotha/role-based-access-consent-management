package com.example.accessconsent;

import java.time.Instant;

public record BreakGlassGrant(
        Long id,
        Long userId,
        Long patientId,
        DataCategory dataCategory,
        AccessAction action,
        PurposeOfUse purpose,
        String reason,
        Instant grantedAt,
        Instant expiresAt
) {
    public BreakGlassGrant {
        if (id == null || id <= 0 || userId == null || userId <= 0
                || patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("IDs must be positive");
        }
        if (dataCategory == null || action == null || purpose == null
                || grantedAt == null || expiresAt == null) {
            throw new IllegalArgumentException("Grant fields are required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Emergency reason is required");
        }
        if (!expiresAt.equals(grantedAt.plusSeconds(3600))) {
            throw new IllegalArgumentException(
                    "Emergency access must expire after one hour");
        }
    }

    public boolean isActiveAt(Instant time) {
        return time != null
                && !time.isBefore(grantedAt)
                && time.isBefore(expiresAt);
    }

    public boolean flagged() {
        return true;
    }

    public boolean permits(DataCategory category, Instant time) {
        return isActiveAt(time)
                && category == dataCategory
                && category != DataCategory.PSYCHOTHERAPY_NOTES
                && category != DataCategory.EMPLOYEE_RECORD
                && category != DataCategory.VIP_RECORD;
    }

    public boolean permitsRequest(AccessRequest request, Instant time) {
        return request != null
                && isActiveAt(time)
                && request.user().id().equals(userId)
                && request.patient().id().equals(patientId)
                && request.dataCategory() == dataCategory
                && request.action() == action
                && request.purpose() == purpose
                && request.dataCategory() != DataCategory.PSYCHOTHERAPY_NOTES
                && request.dataCategory() != DataCategory.EMPLOYEE_RECORD
                && request.dataCategory() != DataCategory.VIP_RECORD
                && !request.patient().employee()
                && !request.patient().vip();
    }
}