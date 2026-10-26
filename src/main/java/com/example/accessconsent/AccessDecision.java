package com.example.accessconsent;

public record AccessDecision(
        AccessOutcome outcome,
        String policy,
        String reason
) {
    public AccessDecision {
        if (outcome == null || policy == null || policy.isBlank()
                || reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Outcome, policy, and reason are required");
        }
    }

    public boolean allowed() {
        return outcome == AccessOutcome.GRANTED;
    }

    public static AccessDecision allow(String policy, String reason) {
        return new AccessDecision(AccessOutcome.GRANTED, policy, reason);
    }

    public static AccessDecision deny(String policy, String reason) {
        return new AccessDecision(AccessOutcome.DENIED, policy, reason);
    }
}