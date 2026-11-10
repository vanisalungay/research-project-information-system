package com.rpis.backend.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * Serializes the structured Risk Management Plan (section 21) of a proposal
 * to/from the {@code risk_management_json} TEXT column. Mirrors the existing
 * {@code objectives_json} / {@code outputs_json} converter convention.
 *
 * <p>The legacy single-text {@code risks} column is deliberately untouched: a
 * proposal saved before the table existed keeps its text and the converter
 * returns an empty list for it, so nothing is lost and no old value is parsed
 * into rows that were never entered.</p>
 */
@Converter
public class RiskManagementListConverter implements AttributeConverter<List<RiskManagementEntry>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<RiskManagementEntry> attribute) {
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
    public List<RiskManagementEntry> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || "[]".equals(dbData.trim())) {
            return new ArrayList<>();
        }
        try {
            List<RiskManagementEntry> parsed = MAPPER.readValue(
                    dbData, new TypeReference<List<RiskManagementEntry>>() {});
            return parsed != null ? parsed : new ArrayList<>();
        } catch (Exception e) {
            // An unreadable value (e.g. the column was filled by hand) is treated as "no
            // structured entries"; the legacy `risks` text of the proposal is still shown
            // by every screen, so nothing disappears from the record.
            return new ArrayList<>();
        }
    }
}
