package com.rpis.backend.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the selected Sustainable Development Goals (SDGs) of a proposal as a
 * JSON array string inside the existing {@code sdg} TEXT column, while exposing
 * them as a {@link List}{@code <String>} to the rest of the application.
 *
 * <p>Legacy rows that stored the SDG as a free-form string (e.g. separated by
 * {@code ;} or newlines) are transparently parsed back into a list so existing
 * data is never lost.</p>
 */
@Converter
public class SdgListConverter implements AttributeConverter<List<String>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
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
        if (dbData == null || dbData.isBlank()) {
            return new ArrayList<>();
        }

        String trimmed = dbData.trim();
        if (trimmed.equals("[]")) {
            return new ArrayList<>();
        }

        try {
            List<String> parsed = MAPPER.readValue(trimmed, new TypeReference<List<String>>() {});
            return parsed != null ? parsed : new ArrayList<>();
        } catch (Exception e) {
            // Legacy fallback: old data was stored as free-form text, possibly
            // separated by semicolons or newlines.
            return splitLegacyValue(trimmed);
        }
    }

    private List<String> splitLegacyValue(String value) {
        List<String> result = new ArrayList<>();
        String normalized = value.replace("\r", "\n");
        String[] parts = normalized.split("[;\\n]");
        for (String part : parts) {
            String t = part.trim();
            if (!t.isEmpty()) {
                result.add(t);
            }
        }
        return result;
    }
}
