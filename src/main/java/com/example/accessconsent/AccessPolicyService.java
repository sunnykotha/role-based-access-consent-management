package com.example.accessconsent;

import java.util.List;

public class AccessPolicyService {

    public AccessDecision evaluate(
            AccessRequest request,
            List<CareTeamAssignment> assignments,
            List<ConsentDirective> consents) {
        return evaluate(request, assignments, consents, List.of());
    }

    public AccessDecision evaluate(
            AccessRequest request,
            List<CareTeamAssignment> assignments,
            List<ConsentDirective> consents,
            List<ProxyAuthorization> proxyAuthorizations) {

        if (request == null || assignments == null || consents == null
                || proxyAuthorizations == null) {
            throw new IllegalArgumentException(
                    "Request, assignments, and consents are required");
        }

        User user = request.user();
        Patient patient = request.patient();
        DataCategory category = request.dataCategory();
        PurposeOfUse purpose = request.purpose();

        if (patient.employee() || patient.vip()
                || category == DataCategory.EMPLOYEE_RECORD
                || category == DataCategory.VIP_RECORD) {
            if (user.role() != UserRole.PRIVACY_OFFICER
                    && user.role() != UserRole.ADMINISTRATOR) {
                return AccessDecision.deny(
                        "EMPLOYEE_VIP_PROTECTION",
                        "Employee and VIP records require a privacy officer or administrator");
            }
        }

        if (category == DataCategory.PSYCHOTHERAPY_NOTES) {
            return AccessDecision.deny(
                    "PSYCHOTHERAPY_NOTES_RESTRICTED",
                    "Psychotherapy notes are not available through ordinary access");
        }

        boolean billingCategory = category == DataCategory.DEMOGRAPHIC
                || category == DataCategory.FINANCIAL;

        if (user.role() == UserRole.BILLING_STAFF) {
            if (request.action() != AccessAction.READ || !billingCategory
                    || purpose != PurposeOfUse.PAYMENT) {
                return AccessDecision.deny(
                        "BILLING_MINIMUM_NECESSARY",
                        "Billing staff may read demographic and financial data for payment");
            }
            return AccessDecision.allow(
                    "BILLING_MINIMUM_NECESSARY",
                    "Request is limited to permitted billing fields");
        }

        boolean proxyRole = user.role() == UserRole.GUARDIAN
                || user.role() == UserRole.PROXY;

        boolean authorizedProxy = proxyRole && proxyAuthorizations.stream()
                .anyMatch(authorization ->
                        authorization.covers(user, patient, request.requestedAt()));

        if (proxyRole && !authorizedProxy) {
            return AccessDecision.deny(
                    "GUARDIAN_PROXY_VALIDATION_REQUIRED",
                    "No active, unrevoked guardian or proxy authorization exists");
        }

        if (proxyRole && request.purpose() != PurposeOfUse.TREATMENT) {
            return AccessDecision.deny(
                    "GUARDIAN_PROXY_PURPOSE_RESTRICTED",
                    "Guardian or proxy access through this policy requires treatment purpose");
        }

        if (user.role() == UserRole.PRIVACY_OFFICER
                || user.role() == UserRole.ADMINISTRATOR) {
            return AccessDecision.allow(
                    "PRIVACY_ADMINISTRATIVE_ACCESS",
                    "Role is permitted by this policy");
        }

        boolean clinicalRole = user.role() == UserRole.CARDIOLOGIST
                || user.role() == UserRole.CLINICIAN
                || user.role() == UserRole.NURSE;

        if (!clinicalRole && !authorizedProxy) {
            return AccessDecision.deny(
                    "ROLE_NOT_PERMITTED",
                    "User role is not permitted to access patient data");
        }

        if (purpose != PurposeOfUse.TREATMENT) {
            return AccessDecision.deny(
                    "CLINICAL_PURPOSE_REQUIRED",
                    "Clinical roles may use this access path for treatment only");
        }

        boolean activeAssignment = assignments.stream()
                .anyMatch(assignment ->
                        assignment.covers(user.id(), patient.id(), request.requestedAt()));

        if (!authorizedProxy && !activeAssignment) {
            return AccessDecision.deny(
                    "ACTIVE_CARE_TEAM_REQUIRED",
                    "No active care-team assignment covers this request");
        }

        boolean sensitive = category == DataCategory.MENTAL_HEALTH
                || category == DataCategory.SUBSTANCE_USE_TREATMENT
                || category == DataCategory.HIV_STATUS;

        if (sensitive) {
            boolean validConsent = consents.stream()
                    .anyMatch(consent -> consent.permits(
                            patient.id(), category, purpose, request.requestedAt()));

            if (!validConsent) {
                return AccessDecision.deny(
                        "SENSITIVE_DATA_CONSENT_REQUIRED",
                        "Sensitive data requires valid, unrevoked consent");
            }
        }

        return AccessDecision.allow(
                "CARE_TEAM_TREATMENT",
                "Active care-team membership and permitted treatment purpose verified");
    }
}