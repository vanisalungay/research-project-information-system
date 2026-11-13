package com.rpis.backend.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Serializes the structured Terminal Report of a reporting stage 4 record to/from the
 * {@code terminal_report_json} TEXT column. Mirrors the existing
 * {@code workplan_json}/{@code objectives_json} converter convention.
 */
@Converter
public class TerminalReportDataConverter implements AttributeConverter<TerminalReportData, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(TerminalReportData attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public TerminalReportData convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, TerminalReportData.class);
        } catch (Exception e) {
            return null;
        }
    }
}
