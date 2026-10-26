package com.example.accessconsent;

import java.time.Instant;
import java.util.List;

public class BreakGlassService {

    public BreakGlassResult createGrant(
            AccessRequest request,
            String reason,
            long grantId,
            long reviewTaskId) {

        validateRequestAndReason(request, reason);

        if (grantId <= 0 || reviewTaskId <= 0) {
            throw new IllegalArgumentException("IDs must be positive");
        }

        User user = request.user();
        Patient patient = request.patient();

        boolean clinician = user.role() == UserRole.CARDIOLOGIST
                || user.role() == UserRole.CLINICIAN
                || user.role() == UserRole.NURSE;

        if (!clinician) {
            throw new IllegalArgumentException(
                    "Break-the-glass access is restricted to clinical roles");
        }
        if (request.purpose() != PurposeOfUse.TREATMENT) {
            throw new IllegalArgumentException(
                    "Emergency access must be for treatment");
        }
        if (request.dataCategory() == DataCategory.PSYCHOTHERAPY_NOTES) {
            throw new IllegalArgumentException(
                    "Psychotherapy notes cannot be accessed through break-the-glass");
        }
        if (patient.employee() || patient.vip()
                || request.dataCategory() == DataCategory.EMPLOYEE_RECORD
                || request.dataCategory() == DataCategory.VIP_RECORD) {
            throw new IllegalArgumentException(
                    "Employee and VIP records require separate privileged review");
        }

        Instant grantedAt = request.requestedAt();
        BreakGlassGrant grant = new BreakGlassGrant(
                grantId,
                user.id(),
                patient.id(),
                request.dataCategory(),
                request.action(),
                request.purpose(),
                reason,
                grantedAt,
                grantedAt.plusSeconds(3600));

        PrivacyReviewTask task = new PrivacyReviewTask(
                reviewTaskId,
                grantId,
                patient.id(),
                grantedAt,
                grantedAt.plusSeconds(24 * 3600L));

        return new BreakGlassResult(grant, task);
    }

    public BreakGlassResult createGrantForOutOfTeamClinician(
            AccessRequest request,
            List<CareTeamAssignment> assignments,
            String reason,
            long grantId,
            long reviewTaskId) {

        if (request == null) {
            throw new IllegalArgumentException("Access request is required");
        }
        if (assignments == null) {
            throw new IllegalArgumentException(
                    "Care-team assignments are required for this check");
        }

        for (CareTeamAssignment assignment : assignments) {
            if (assignment == null) {
                throw new IllegalArgumentException(
                        "Care-team assignments cannot contain null entries");
            }

            if (assignment.covers(
                    request.user().id(),
                    request.patient().id(),
                    request.requestedAt())) {
                throw new IllegalArgumentException(
                        "Break-the-glass access is for clinicians outside the active care team");
            }
        }

        return createGrant(request, reason, grantId, reviewTaskId);
    }

    private void validateRequestAndReason(
            AccessRequest request,
            String reason) {
        if (request == null) {
            throw new IllegalArgumentException("Access request is required");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "A stated emergency reason is required");
        }
    }
}