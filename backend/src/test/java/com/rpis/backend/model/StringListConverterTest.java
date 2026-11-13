package com.rpis.backend.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the stable identifiers stored on a Proposal Announcement (RIIDE
 * program keys, reminder ids) round-trip through the {@code StringListConverter}
 * backing the new TEXT columns.
 */
class StringListConverterTest {

    private final StringListConverter converter = new StringListConverter();

    @Test
    void roundTripsSelectedIdentifiers() {
        List<String> selected = List.of("dagat", "kalikasan");

        String json = converter.convertToDatabaseColumn(selected);
        assertNotNull(json);

        assertEquals(selected, converter.convertToEntityAttribute(json));
    }

    @Test
    void roundTripsASingleIdentifier() {
        String json = converter.convertToDatabaseColumn(List.of("research-plan"));
        assertEquals(List.of("research-plan"), converter.convertToEntityAttribute(json));
    }

    @Test
    void nullAttributeYieldsAnEmptyJsonArray() {
        assertEquals("[]", converter.convertToDatabaseColumn(null));
    }

    @Test
    void nullOrBlankColumnYieldsAnEmptyList() {
        // Announcements created before the columns existed have NULL values.
        assertTrue(converter.convertToEntityAttribute(null).isEmpty());
        assertTrue(converter.convertToEntityAttribute("   ").isEmpty());
        assertTrue(converter.convertToEntityAttribute("[]").isEmpty());
    }

    @Test
    void unreadableColumnYieldsAnEmptyListInsteadOfFailing() {
        assertTrue(converter.convertToEntityAttribute("not-json").isEmpty());
    }
}
