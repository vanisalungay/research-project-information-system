package com.rpis.backend.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The persistence round-trip of the structured Risk Management Plan (section 21): what is
 * written to the {@code risk_management_json} column is exactly what is read back, and a
 * proposal that predates the table (NULL, or an unreadable value) simply has no structured
 * entries instead of failing.
 */
class RiskManagementListConverterTest {

    private final RiskManagementListConverter converter = new RiskManagementListConverter();

    @Test
    void entriesSurviveTheRoundTripWithTheirOwnFields() {
        List<RiskManagementEntry> entries = List.of(
                new RiskManagementEntry("Delayed delivery of research equipment", "High", "Medium",
                        "Identify alternative suppliers."),
                new RiskManagementEntry("Low participation", "Low", "High", "Hold assemblies."));

        String stored = converter.convertToDatabaseColumn(entries);
        List<RiskManagementEntry> read = converter.convertToEntityAttribute(stored);

        assertEquals(2, read.size());
        assertEquals("Delayed delivery of research equipment", read.get(0).getIdentifiedRisks());
        assertEquals("High", read.get(0).getLikelihood());
        assertEquals("Medium", read.get(0).getImpact());
        assertEquals("Identify alternative suppliers.", read.get(0).getMitigationMeasure());
        assertEquals("Low participation", read.get(1).getIdentifiedRisks());
        assertEquals("Low", read.get(1).getLikelihood());
        assertEquals("High", read.get(1).getImpact());
        assertEquals("Hold assemblies.", read.get(1).getMitigationMeasure());
    }

    @Test
    void aProposalThatPredatesTheTableHasNoStructuredEntries() {
        assertTrue(converter.convertToEntityAttribute(null).isEmpty());
        assertTrue(converter.convertToEntityAttribute("").isEmpty());
        assertTrue(converter.convertToEntityAttribute("[]").isEmpty());
        assertTrue(converter.convertToEntityAttribute("not json at all").isEmpty(),
                "an unreadable value never breaks the record");
    }

    @Test
    void anEmptyPlanIsStoredAsAnEmptyJsonArray() {
        assertEquals("[]", converter.convertToDatabaseColumn(List.of()));
        assertEquals("[]", converter.convertToDatabaseColumn(null));
    }
}
