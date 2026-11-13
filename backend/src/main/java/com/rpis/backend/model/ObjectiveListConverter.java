package com.rpis.backend.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializes the list of objective rows of a Quarterly Progress Report to/from
 * the {@code objectives_json} TEXT column.
 */
@Converter
public class ObjectiveListConverter implements AttributeConverter<List<QuarterlyObjective>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<QuarterlyObjective> attribute) {
        if (attribute == null) {
            return "[]";
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Override
    public List<QuarterlyObjective> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || "[]".equals(dbData.trim())) {
            return new ArrayList<>();
        }
        try {
            List<QuarterlyObjective> parsed = MAPPER.readValue(
                    dbData, new TypeReference<List<QuarterlyObjective>>() {});
            return parsed != null ? parsed : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
