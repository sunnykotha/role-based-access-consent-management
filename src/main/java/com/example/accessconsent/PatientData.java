package com.example.accessconsent;

import java.util.Map;

public record PatientData(Long patientId, Map<DataCategory, String> fields) {
    public PatientData {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException("Patient ID must be positive");
        }
        if (fields == null || fields.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == null || entry.getValue() == null)) {
            throw new IllegalArgumentException("Fields must not contain null keys or values");
        }
        fields = Map.copyOf(fields);
    }
}