package com.example.accessconsent;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record ConsentDirective(
        Long id,
        Long patientId,
        Set<DataCategory> categories,
        Set<PurposeOfUse> purposes,
        Instant validFrom,
        Instant validUntil,
        Instant revokedAt) {

    public ConsentDirective {
        if (id == null || id <= 0 || patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Consent and patient ids must be positive");
        }
        categories = Set.copyOf(Objects.requireNonNull(categories, "Categories are required"));
        purposes = Set.copyOf(Objects.requireNonNull(purposes, "Purposes are required"));
        if (categories.isEmpty() || purposes.isEmpty()) {
            throw new IllegalArgumentException("Consent scope cannot be empty");
        }
        Objects.requireNonNull(validFrom, "Consent start is required");
        if (validUntil != null && !validUntil.isAfter(validFrom)) {
            throw new IllegalArgumentException("Consent end must be after start");
        }
        if (revokedAt != null && revokedAt.isBefore(validFrom)) {
            throw new IllegalArgumentException("Revocation cannot precede consent");
        }
    }

    public boolean permits(
            Long requestedPatientId,
            DataCategory category,
            PurposeOfUse purpose,
            Instant now) {
        Objects.requireNonNull(category, "Data category is required");
        Objects.requireNonNull(purpose, "Purpose is required");
        Objects.requireNonNull(now, "Evaluation time is required");

        return patientId.equals(requestedPatientId)
                && categories.contains(category)
                && purposes.contains(purpose)
                && !now.isBefore(validFrom)
                && (validUntil == null || now.isBefore(validUntil))
                && (revokedAt == null || now.isBefore(revokedAt));
    }
}