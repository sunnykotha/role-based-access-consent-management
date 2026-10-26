package com.example.accessconsent;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FieldMaskingServiceTest {

    private final FieldMaskingService service = new FieldMaskingService();

    @Test
    void billingStaffSeeOnlyDemographicAndFinancialFields() {
        User billingUser = new User(1L, "Billing User", UserRole.BILLING_STAFF);
        PatientData data = new PatientData(10L, Map.of(
                DataCategory.DEMOGRAPHIC, "Demographic details",
                DataCategory.FINANCIAL, "Billing details",
                DataCategory.CLINICAL_NOTES, "Clinical notes",
                DataCategory.HIV_STATUS, "HIV status"
        ));

        Map<DataCategory, String> visible = service.visibleFields(billingUser, data);

        assertEquals(2, visible.size());
        assertTrue(visible.containsKey(DataCategory.DEMOGRAPHIC));
        assertTrue(visible.containsKey(DataCategory.FINANCIAL));
        assertFalse(visible.containsKey(DataCategory.CLINICAL_NOTES));
        assertFalse(visible.containsKey(DataCategory.HIV_STATUS));
    }

    @Test
    void psychotherapyNotesRemainMasked() {
        User clinician = new User(2L, "Clinician", UserRole.CLINICIAN);
        PatientData data = new PatientData(10L, Map.of(
                DataCategory.DEMOGRAPHIC, "Demographic details",
                DataCategory.CLINICAL_NOTES, "Clinical notes",
                DataCategory.PSYCHOTHERAPY_NOTES, "Psychotherapy notes"
        ));

        Map<DataCategory, String> visible = service.visibleFields(clinician, data);

        assertTrue(visible.containsKey(DataCategory.CLINICAL_NOTES));
        assertFalse(visible.containsKey(DataCategory.PSYCHOTHERAPY_NOTES));
    }

    @Test
    void rejectsMissingUserOrPatientData() {
        User clinician = new User(2L, "Clinician", UserRole.CLINICIAN);
        PatientData data = new PatientData(10L, Map.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.visibleFields(null, data));
        assertThrows(IllegalArgumentException.class,
                () -> service.visibleFields(clinician, null));
    }
}