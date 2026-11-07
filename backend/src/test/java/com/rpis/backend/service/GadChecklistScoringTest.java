package com.rpis.backend.service;

import com.rpis.backend.model.GadChecklistData;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The GAD Checklist 2 scoring is the backend's authority: the total, interpretation and
 * completion state are always recomputed from the responses (never taken from the client),
 * using the official scales and the stated maximum of 20.
 */
class GadChecklistScoringTest {

    private static final String[] ITEMS = {
        "1.1", "1.2", "2.0", "3.1", "3.2", "4.0", "5.0",
        "6.1", "6.2", "6.3", "7.0", "8.0", "9.1", "9.2", "10.1", "10.2", "10.3"
    };

    private static GadChecklistData withResponses(String... keysAndValues) {
        GadChecklistData data = new GadChecklistData();
        Map<String, String> responses = new LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            responses.put(keysAndValues[i], keysAndValues[i + 1]);
        }
        data.setResponses(responses);
        return data;
    }

    private static GadChecklistData allOf(String response) {
        String[] flat = new String[ITEMS.length * 2];
        for (int i = 0; i < ITEMS.length; i++) {
            flat[i * 2] = ITEMS[i];
            flat[i * 2 + 1] = response;
        }
        return withResponses(flat);
    }

    @Test
    void scoresZeroForAllNoAndTwentyForAllYes() {
        GadChecklistData no = allOf("NO");
        GadChecklistScoring.recalculate(no);
        assertEquals(new BigDecimal("0.00"), no.getTotalScore());
        assertTrue(no.getCompleted());
        assertTrue(no.getInterpretation().contains("invisible"));

        GadChecklistData yes = allOf("YES");
        GadChecklistScoring.recalculate(yes);
        assertEquals(new BigDecimal("20.00"), yes.getTotalScore());
        assertTrue(yes.getCompleted());
        assertTrue(yes.getInterpretation().contains("gender-responsive"));
    }

    @Test
    void keepsTheOfficialSixPointThreeValue() {
        GadChecklistData data = withResponses("6.1", "YES", "6.2", "YES", "6.3", "YES");
        GadChecklistScoring.recalculate(data);
        // 0.67 + 0.67 + 0.66 = 2.00, the criterion's stated maximum.
        assertEquals(new BigDecimal("2.00"), data.getTotalScore());
    }

    @Test
    void neverTrustsAClientTotal() {
        GadChecklistData data = allOf("NO");
        data.setTotalScore(new BigDecimal("19.99"));
        data.setInterpretation("gender-responsive");
        data.setCompleted(true);

        GadChecklistScoring.recalculate(data);

        assertEquals(new BigDecimal("0.00"), data.getTotalScore());
        assertTrue(data.getInterpretation().contains("invisible"));
    }

    @Test
    void anIncompleteChecklistIsNotFinal() {
        GadChecklistData data = withResponses("1.1", "YES");
        GadChecklistScoring.recalculate(data);
        assertFalse(data.getCompleted());
        assertEquals("", data.getInterpretation());
        assertEquals(new BigDecimal("1.00"), data.getTotalScore());
        assertFalse(GadChecklistScoring.isComplete(data));
    }

    @Test
    void ignoresAnUnknownResponseOrItem() {
        GadChecklistData data = withResponses("1.1", "MAYBE", "9.9", "YES");
        GadChecklistScoring.recalculate(data);
        assertFalse(data.getCompleted());
        assertEquals(new BigDecimal("0.00"), data.getTotalScore());
    }
}
