package com.example.accessconsent;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PatientDataTest {

    @Test
    void rejectsNonPositivePatientId() {
        assertThrows(IllegalArgumentException.class,
                () -> new PatientData(0L, Map.of()));
    }

    @Test
    void copiesInputFieldsToPreventExternalMutation() {
        Map<DataCategory, String> fields =
                new java.util.EnumMap<>(DataCategory.class);
        fields.put(DataCategory.DEMOGRAPHIC, "Patient demographics");

        PatientData data = new PatientData(1L, fields);
        fields.put(DataCategory.CLINICAL_NOTES, "Clinical notes");

        assertFalse(data.fields().containsKey(DataCategory.CLINICAL_NOTES));
    }
}