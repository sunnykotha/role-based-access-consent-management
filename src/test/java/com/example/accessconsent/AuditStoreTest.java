package com.example.accessconsent;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuditStoreTest {

    private AuditEvent event(long id, long patientId) {
        return new AuditEvent(
                id,
                1L,
                patientId,
                DataCategory.CLINICAL_NOTES,
                AccessAction.READ,
                PurposeOfUse.TREATMENT,
                Instant.parse("2026-01-01T10:00:00Z"),
                "Emergency treatment",
                AccessOutcome.GRANTED,
                "BREAK_GLASS",
                true);
    }

    @Test
    void appendsAndRetrievesAuditEvents() {
        AuditStore store = new AuditStore();
        AuditEvent event = event(1L, 10L);

        store.append(event);

        assertEquals(List.of(event), store.allEvents());
        assertEquals(List.of(event), store.eventsForPatient(10L));
    }

    @Test
    void returnedEventListCannotBeModified() {
        AuditStore store = new AuditStore();
        store.append(event(1L, 10L));

        List<AuditEvent> events = store.allEvents();

        assertThrows(UnsupportedOperationException.class,
                () -> events.add(event(2L, 10L)));
        assertThrows(UnsupportedOperationException.class,
                () -> store.eventsForPatient(10L).clear());
        assertEquals(1, store.allEvents().size());
    }

    @Test
    void rejectsDuplicateEventIds() {
        AuditStore store = new AuditStore();
        store.append(event(1L, 10L));

        assertThrows(IllegalArgumentException.class,
                () -> store.append(event(1L, 11L)));
        assertEquals(1, store.allEvents().size());
    }

    @Test
    void recordsGrantedAndDeniedOutcomes() {
        AuditStore store = new AuditStore();

        AuditEvent granted = event(1L, 10L);
        AuditEvent denied = new AuditEvent(
                2L, 1L, 10L,
                DataCategory.HIV_STATUS,
                AccessAction.READ,
                PurposeOfUse.TREATMENT,
                Instant.parse("2026-01-01T10:01:00Z"),
                "Consent missing",
                AccessOutcome.DENIED,
                "SENSITIVE_DATA_CONSENT_REQUIRED",
                false);

        store.append(granted);
        store.append(denied);

        assertEquals(AccessOutcome.GRANTED, store.allEvents().get(0).outcome());
        assertEquals(AccessOutcome.DENIED, store.allEvents().get(1).outcome());
    }

    @Test
    void filtersEventsByPatient() {
        AuditStore store = new AuditStore();
        store.append(event(1L, 10L));
        store.append(event(2L, 20L));

        assertEquals(1, store.eventsForPatient(10L).size());
        assertEquals(20L, store.eventsForPatient(20L).get(0).patientId());
    }

    @Test
    void rejectsNullEventsAndInvalidPatientIds() {
        AuditStore store = new AuditStore();

        assertThrows(IllegalArgumentException.class,
                () -> store.append(null));
        assertThrows(IllegalArgumentException.class,
                () -> store.eventsForPatient(0L));
    }
}