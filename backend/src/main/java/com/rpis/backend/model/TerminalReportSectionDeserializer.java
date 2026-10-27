package com.rpis.backend.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads a body section of the Terminal Report, accepting both the current shape
 * ({@code {"text": ..., "subsections": [...], "tables": [...]}}) and the plain string a
 * draft could hold before body sections carried subsections and tables. A string is read as
 * the section text, so a draft saved earlier is never lost when it is reopened.
 */
public class TerminalReportSectionDeserializer extends JsonDeserializer<TerminalReportSection> {

    @Override
    public TerminalReportSection deserialize(JsonParser parser, DeserializationContext context)
            throws IOException {
        JsonNode node = parser.readValueAsTree();
        TerminalReportSection section = new TerminalReportSection();
        if (node == null || node.isNull()) {
            return section;
        }
        if (node.isTextual()) {
            section.setText(node.asText());
            return section;
        }

        section.setText(node.hasNonNull("text") ? node.get("text").asText() : null);
        for (JsonNode subsection : node.path("subsections")) {
            section.getSubsections().add(new TerminalReportSubsection(
                    subsection.path("heading").asText(null),
                    subsection.path("text").asText(null)));
        }
        for (JsonNode table : node.path("tables")) {
            section.getTables().add(readTable(table));
        }
        return section;
    }

    static TerminalReportTable readTable(JsonNode node) {
        TerminalReportTable table = new TerminalReportTable();
        table.setCaption(node.path("caption").asText(null));
        table.setNote(node.path("note").asText(null));
        List<String> columns = new ArrayList<>();
        for (JsonNode column : node.path("columns")) {
            columns.add(column.asText(""));
        }
        table.setColumns(columns);
        List<List<String>> rows = new ArrayList<>();
        for (JsonNode row : node.path("rows")) {
            List<String> cells = new ArrayList<>();
            for (JsonNode cell : row) {
                cells.add(cell.asText(""));
            }
            rows.add(cells);
        }
        table.setRows(rows);
        return table;
    }
}
