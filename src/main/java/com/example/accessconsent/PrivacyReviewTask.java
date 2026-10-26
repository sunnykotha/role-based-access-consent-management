package com.example.accessconsent;

import java.time.Instant;

public record PrivacyReviewTask(
        Long id,
        Long grantId,
        Long patientId,
        Instant createdAt,
        Instant dueAt
) {
    public PrivacyReviewTask {
        if (id == null || id <= 0 || grantId == null || grantId <= 0
                || patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Task IDs must be positive");
        }
        if (createdAt == null || dueAt == null
                || !dueAt.equals(createdAt.plusSeconds(24 * 3600L))) {
            throw new IllegalArgumentException(
                    "Privacy review must be due within 24 hours");
        }
    }
}