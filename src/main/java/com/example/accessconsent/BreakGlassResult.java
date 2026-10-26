package com.example.accessconsent;

public record BreakGlassResult(
        BreakGlassGrant grant,
        PrivacyReviewTask reviewTask
) {
    public BreakGlassResult {
        if (grant == null || reviewTask == null
                || !grant.id().equals(reviewTask.grantId())
                || !grant.patientId().equals(reviewTask.patientId())) {
            throw new IllegalArgumentException(
                    "Grant and matching privacy review task are required");
        }
    }
}