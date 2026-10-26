package com.example.accessconsent;

import java.time.Instant;
import java.util.Objects;

public record AccessRequest(
        Long id,
        User user,
        Patient patient,
        DataCategory dataCategory,
        AccessAction action,
        PurposeOfUse purpose,
        Instant requestedAt) {

    public AccessRequest {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Access request id must be positive");
        }
        Objects.requireNonNull(user, "User is required");
        Objects.requireNonNull(patient, "Patient is required");
        Objects.requireNonNull(dataCategory, "Data category is required");
        Objects.requireNonNull(action, "Access action is required");
        Objects.requireNonNull(purpose, "Purpose of use is required");
        Objects.requireNonNull(requestedAt, "Request time is required");
    }
}