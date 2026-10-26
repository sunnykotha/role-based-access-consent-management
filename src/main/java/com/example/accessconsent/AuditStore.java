package com.example.accessconsent;

import java.util.ArrayList;
import java.util.List;

public class AuditStore {

    private final List<AuditEvent> events = new ArrayList<>();

    public synchronized void append(AuditEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Audit event is required");
        }
        if (events.stream().anyMatch(existing ->
                existing.id().equals(event.id()))) {
            throw new IllegalArgumentException(
                    "Audit event ID must be unique");
        }
        events.add(event);
    }

    public synchronized List<AuditEvent> allEvents() {
        return List.copyOf(events);
    }

    public synchronized List<AuditEvent> eventsForPatient(Long patientId) {
        if (patientId == null || patientId <= 0) {
            throw new IllegalArgumentException(
                    "Patient ID must be positive");
        }
        return events.stream()
                .filter(event -> event.patientId().equals(patientId))
                .toList();
    }
}