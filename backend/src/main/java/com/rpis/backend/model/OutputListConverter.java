package com.rpis.backend.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializes the list of expected-output rows (6Ps) of a Quarterly Progress
 * Report to/from the {@code outputs_json} TEXT column.
 */
@Converter
public class OutputListConverter implements AttributeConverter<List<QuarterlyOutput>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<QuarterlyOutput> attribute) {
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
    public List<QuarterlyOutput> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || "[]".equals(dbData.trim())) {
            return new ArrayList<>();
        }
        try {
            List<QuarterlyOutput> parsed = MAPPER.readValue(
                    dbData, new TypeReference<List<QuarterlyOutput>>() {});
            return parsed != null ? parsed : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
