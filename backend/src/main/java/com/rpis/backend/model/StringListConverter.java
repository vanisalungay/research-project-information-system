package com.rpis.backend.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializes a list of stable string identifiers — the RIIDE priority-agenda
 * program keys and the standard announcement reminder ids of a Proposal
 * Announcement — to/from a TEXT column holding a JSON array.
 * <p>
 * Follows the same persistence pattern as {@link ObjectiveListConverter} and
 * {@link WorkplanDataConverter}: a null or unreadable column yields an empty
 * list, so announcement rows created before these columns existed stay readable
 * and editable.
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
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
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || "[]".equals(dbData.trim())) {
            return new ArrayList<>();
        }
        try {
            List<String> parsed = MAPPER.readValue(dbData, new TypeReference<List<String>>() {});
            return parsed != null ? parsed : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
