package com.example.accessconsent;

import java.time.Instant;
import java.util.Objects;

public record CareTeamAssignment(
        Long userId,
        Long patientId,
        Instant validFrom,
        Instant validUntil) {

    public CareTeamAssignment {
        if (userId == null || userId <= 0 || patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("User and patient ids must be positive");
        }
        Objects.requireNonNull(validFrom, "Assignment start is required");
        if (validUntil != null && !validUntil.isAfter(validFrom)) {
            throw new IllegalArgumentException("Assignment end must be after start");
        }
    }

    public boolean isActiveAt(Instant time) {
        Objects.requireNonNull(time, "Evaluation time is required");
        return !time.isBefore(validFrom)
                && (validUntil == null || time.isBefore(validUntil));
    }

    public boolean covers(Long requestedUserId, Long requestedPatientId, Instant time) {
        return userId.equals(requestedUserId)
                && patientId.equals(requestedPatientId)
                && isActiveAt(time);
    }
}