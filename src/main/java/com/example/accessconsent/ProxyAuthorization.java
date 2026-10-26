package com.example.accessconsent;

import java.time.Instant;

public record ProxyAuthorization(
        Long id,
        Long userId,
        Long patientId,
        Instant validFrom,
        Instant validUntil,
        Instant revokedAt
) {
    public ProxyAuthorization {
        if (id == null || id <= 0 || userId == null || userId <= 0
                || patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("IDs must be positive");
        }
        if (validFrom == null || validUntil == null
                || !validUntil.isAfter(validFrom)) {
            throw new IllegalArgumentException("Invalid authorization period");
        }
    }

    public boolean isActiveAt(Instant time) {
        return time != null
                && !time.isBefore(validFrom)
                && time.isBefore(validUntil)
                && (revokedAt == null || time.isBefore(revokedAt));
    }

    public boolean covers(User user, Patient patient, Instant time) {
        return user != null && patient != null
                && user.id().equals(userId)
                && patient.id().equals(patientId)
                && isActiveAt(time);
    }
}