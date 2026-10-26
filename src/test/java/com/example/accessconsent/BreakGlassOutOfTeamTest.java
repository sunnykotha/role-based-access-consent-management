package com.example.accessconsent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class BreakGlassOutOfTeamTest {

    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");
    private final User clinician =
            new User(1L, "Emergency Clinician", UserRole.CLINICIAN);
    private final Patient patient =
            new Patient(10L, "Test Patient", 40, false, false);
    private final BreakGlassService service = new BreakGlassService();

    private AccessRequest request() {
        return new AccessRequest(
                100L,
                clinician,
                patient,
                DataCategory.CLINICAL_NOTES,
                AccessAction.READ,
                PurposeOfUse.TREATMENT,
                now);
    }

    @Test
    void createsGrantWhenClinicianIsOutsideCareTeam() {
        BreakGlassResult result = service.createGrantForOutOfTeamClinician(
                request(), List.of(), "Emergency treatment", 200L, 300L);

        assertEquals(200L, result.grant().id());
        assertEquals(now.plusSeconds(3600), result.grant().expiresAt());
        assertEquals(now.plusSeconds(24 * 3600L), result.reviewTask().dueAt());
    }

    @Test
    void rejectsClinicianWithActiveCareTeamAssignment() {
        CareTeamAssignment assignment = new CareTeamAssignment(
                clinician.id(),
                patient.id(),
                now.minusSeconds(60),
                now.plusSeconds(3600));

        assertThrows(IllegalArgumentException.class,
                () -> service.createGrantForOutOfTeamClinician(
                        request(), List.of(assignment),
                        "Emergency treatment", 200L, 300L));
    }

    @Test
    void allowsGrantWhenAssignmentHasExpired() {
        CareTeamAssignment expiredAssignment = new CareTeamAssignment(
                clinician.id(),
                patient.id(),
                now.minusSeconds(3600),
                now);

        BreakGlassResult result = service.createGrantForOutOfTeamClinician(
                request(), List.of(expiredAssignment),
                "Emergency treatment", 200L, 300L);

        assertEquals(200L, result.grant().id());
    }

    @Test
    void rejectsMissingAssignmentList() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createGrantForOutOfTeamClinician(
                        request(), null,
                        "Emergency treatment", 200L, 300L));
    }
}