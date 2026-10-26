package com.example.accessconsent;

import java.util.Map;
import java.util.EnumMap;

public class FieldMaskingService {

    public Map<DataCategory, String> visibleFields(User user, PatientData patientData) {
        if (user == null || patientData == null) {
            throw new IllegalArgumentException("User and patient data are required");
        }

        Map<DataCategory, String> visible = new EnumMap<>(DataCategory.class);

        patientData.fields().forEach((category, value) -> {
            if (user.role() == UserRole.BILLING_STAFF) {
                if (category == DataCategory.DEMOGRAPHIC
                        || category == DataCategory.FINANCIAL) {
                    visible.put(category, value);
                }
            } else if (category != DataCategory.PSYCHOTHERAPY_NOTES) {
                visible.put(category, value);
            }
        });

        return Map.copyOf(visible);
    }
}