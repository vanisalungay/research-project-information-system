package com.rpis.backend.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Serializes the structured Project Workplan of a quarterly progress report
 * to/from the {@code workplan_json} TEXT column. Mirrors the existing
 * {@code objectives_json}/{@code outputs_json} converter convention.
 */
@Converter
public class WorkplanDataConverter implements AttributeConverter<WorkplanData, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(WorkplanData attribute) {
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
    public WorkplanData convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(dbData, WorkplanData.class);
        } catch (Exception e) {
            return null;
        }
    }
}
