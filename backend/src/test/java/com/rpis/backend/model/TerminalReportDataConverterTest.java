package com.rpis.backend.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The stored JSON of a Terminal Report is read back into the current shape, including drafts
 * saved before body sections carried subsections and tables (a plain string section), so a
 * reopened draft is never lost.
 */
class TerminalReportDataConverterTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final TerminalReportDataConverter converter = new TerminalReportDataConverter();

    @Test
    void readsASectionWrittenAsSubsectionsAndTables() throws Exception {
        TerminalReportData data = mapper.readValue(
                "{\"projectTitle\":\"Fish Landing Survey\","
                        + "\"resultsAndDiscussion\":{\"text\":\"\",\"subsections\":"
                        + "[{\"heading\":\"Landings\",\"text\":\"24 species were recorded.\"}],"
                        + "\"tables\":[{\"caption\":\"Catch\",\"note\":\"Source: records.\","
                        + "\"columns\":[\"Site\",\"Total\"],\"rows\":[[\"Poblacion\",\"12\"]]}]},"
                        + "\"keywords\":\"fish, landings\"}",
                TerminalReportData.class);

        assertEquals("Fish Landing Survey", data.getProjectTitle());
        assertEquals("fish, landings", data.getKeywords());
        assertEquals("Landings", data.getResultsAndDiscussion().getSubsections().get(0).getHeading());
        assertEquals("24 species were recorded.",
                data.getResultsAndDiscussion().getSubsections().get(0).getText());
        TerminalReportTable table = data.getResultsAndDiscussion().getTables().get(0);
        assertEquals("Catch", table.getCaption());
        assertEquals(List.of("Site", "Total"), table.getColumns());
        assertEquals(List.of(List.of("Poblacion", "12")), table.getRows());
    }

    @Test
    void readsASectionSavedAsPlainTextAsTheSectionText() throws Exception {
        TerminalReportData data = mapper.readValue(
                "{\"introduction\":\"Fish landing surveys document catch composition.\"}",
                TerminalReportData.class);

        assertEquals("Fish landing surveys document catch composition.",
                data.getIntroduction().getText());
        assertTrue(data.getIntroduction().getSubsections().isEmpty());
        assertTrue(data.getIntroduction().hasText());
    }

    @Test
    void roundTripsThroughTheStoredColumn() {
        TerminalReportData data = new TerminalReportData();
        data.setProjectTitle("Fish Landing Survey");
        data.setResultsAndDiscussion(new TerminalReportSection());
        data.getResultsAndDiscussion().setSubsections(
                new java.util.ArrayList<>(List.of(new TerminalReportSubsection("Landings", "24 species."))));
        data.setLiteratureCited(
                new java.util.ArrayList<>(List.of(new TerminalReportReference(
                        "Dela Cruz, J.", "2020", "Landing survey methods", "", "Naawan", "", "12-18"))));

        String stored = converter.convertToDatabaseColumn(data);
        TerminalReportData reloaded = converter.convertToEntityAttribute(stored);

        assertNotNull(reloaded);
        assertEquals("Fish Landing Survey", reloaded.getProjectTitle());
        assertEquals("Landings", reloaded.getResultsAndDiscussion().getSubsections().get(0).getHeading());
        assertEquals("Dela Cruz, J.", reloaded.getLiteratureCited().get(0).getAuthor());
    }
}
