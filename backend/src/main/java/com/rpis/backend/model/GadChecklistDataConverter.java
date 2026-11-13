package com.rpis.backend.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Serializes the interactive GAD Checklist 2 of a proposal to/from the
 * {@code gad_checklist_json} TEXT column. Mirrors the existing
 * {@code terminal_report_json} / {@code workplan_json} converter convention.
 */
@Converter
public class GadChecklistDataConverter implements AttributeConverter<GadChecklistData, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(GadChecklistData attribute) {
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
    public GadChecklistData convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, GadChecklistData.class);
        } catch (Exception e) {
            return null;
        }
    }
}
