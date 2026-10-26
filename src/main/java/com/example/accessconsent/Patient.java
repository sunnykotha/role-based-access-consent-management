package com.example.accessconsent;

public record Patient(Long id, String name, int ageYears, boolean employee, boolean vip) {
    public Patient {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Patient id must be positive");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Patient name is required");
        }
        if (ageYears < 0 || ageYears > 130) {
            throw new IllegalArgumentException("Patient age is invalid");
        }
    }
}