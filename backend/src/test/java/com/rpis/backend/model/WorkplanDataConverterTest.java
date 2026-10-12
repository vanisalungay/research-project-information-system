package com.rpis.backend.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies that the structured Project Workplan round-trips through the
 * {@code workplan_json} converter (the persistence mechanism for the
 * interactive workplan form).
 */
class WorkplanDataConverterTest {

    private final WorkplanDataConverter converter = new WorkplanDataConverter();

    @Test
    void roundTripsStructuredWorkplan() {
        WorkplanData wp = new WorkplanData();
        wp.setAsOf("2026-03-31");
        wp.setProjectTitle("Fish Diversity Survey");
        wp.setProjectLeader("Juan Dela Cruz");
        wp.setProjectDuration("12");
        wp.setRemarks("Catch up plan: extend the field survey in June.");
        wp.setObjectives(List.of(
                new WorkplanObjective("Identify the different fishes",
                        List.of("Process PRs", "Field survey"), "25%"),
                new WorkplanObjective("Document landing sites", List.of(), "50%")));

        String json = converter.convertToDatabaseColumn(wp);
        assertNotNull(json);

        WorkplanData back = converter.convertToEntityAttribute(json);
        assertNotNull(back);
        assertEquals("2026-03-31", back.getAsOf());
        assertEquals("Fish Diversity Survey", back.getProjectTitle());
        assertEquals("Juan Dela Cruz", back.getProjectLeader());
        assertEquals("12", back.getProjectDuration());
        assertEquals(2, back.getObjectives().size());
        assertEquals("Identify the different fishes", back.getObjectives().get(0).getObjective());
        assertEquals("Field survey", back.getObjectives().get(0).getMonths().get(1));
        assertEquals("25%", back.getObjectives().get(0).getTotal());
        assertEquals("Catch up plan: extend the field survey in June.", back.getRemarks());
    }

    @Test
    void blankOrNullColumnYieldsNullWorkplan() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute("   "));
    }

    @Test
    void nullAttributeYieldsNullColumn() {
        assertNull(converter.convertToDatabaseColumn(null));
    }
}
