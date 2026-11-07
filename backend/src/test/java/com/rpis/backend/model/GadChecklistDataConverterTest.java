package com.rpis.backend.model;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The GAD Checklist 2 of a proposal survives the JSON round trip of the
 * {@code gad_checklist_json} column, and a missing column reads as "no checklist" (a proposal
 * that predates the checklist keeps its uploaded document untouched).
 */
class GadChecklistDataConverterTest {

    private final GadChecklistDataConverter converter = new GadChecklistDataConverter();

    @Test
    void roundTripsThroughTheStoredColumn() {
        GadChecklistData data = new GadChecklistData();
        Map<String, String> responses = new LinkedHashMap<>();
        responses.put("1.1", "YES");
        responses.put("6.3", "PARTLY");
        data.setResponses(responses);
        Map<String, String> comments = new LinkedHashMap<>();
        comments.put("1.1", "Discussed with the women.");
        data.setComments(comments);

        String stored = converter.convertToDatabaseColumn(data);
        GadChecklistData reloaded = converter.convertToEntityAttribute(stored);

        assertNotNull(reloaded);
        assertEquals("YES", reloaded.getResponses().get("1.1"));
        assertEquals("PARTLY", reloaded.getResponses().get("6.3"));
        assertEquals("Discussed with the women.", reloaded.getComments().get("1.1"));
    }

    @Test
    void aBlankOrNullColumnReadsAsNoChecklist() {
        assertNull(converter.convertToEntityAttribute(null));
        assertNull(converter.convertToEntityAttribute(""));
        assertNull(converter.convertToDatabaseColumn(null));
    }
}
