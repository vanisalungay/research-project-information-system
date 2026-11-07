package com.rpis.backend.service;

import com.rpis.backend.model.GadChecklistData;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The authoritative scoring of Section 19 - the GAD Checklist 2 (Project Identification and
 * Design Stages).
 *
 * <p>The client never decides the total: {@link #recalculate(GadChecklistData)} recomputes the
 * criterion subtotals, the total GAD score and the interpretation from the responses using the
 * official scales, exactly like the frontend's {@code utils/gadChecklist.js}. Scores are kept
 * in hundredths of a point so the arithmetic is exact (no floating-point drift), and every
 * criterion subtotal is bounded by the official maximum of 2 so the total never exceeds 20.</p>
 */
public final class GadChecklistScoring {

    private GadChecklistScoring() {
    }

    /** Scores in hundredths of a point: 0.33 -&gt; 33, 1.0 -&gt; 100. */
    private static final Map<String, Map<String, Integer>> SCALES = Map.of(
            "HALF", Map.of("NO", 0, "PARTLY", 50, "YES", 100),
            "FULL", Map.of("NO", 0, "PARTLY", 100, "YES", 200),
            "THIRD", Map.of("NO", 0, "PARTLY", 33, "YES", 67),
            "THIRD_66", Map.of("NO", 0, "PARTLY", 33, "YES", 66));

    /** The scale of every one of the 17 assessment items, in document order. */
    private static final Map<String, String> ITEM_SCALE = new LinkedHashMap<>();

    static {
        ITEM_SCALE.put("1.1", "HALF");
        ITEM_SCALE.put("1.2", "HALF");
        ITEM_SCALE.put("2.0", "FULL");
        ITEM_SCALE.put("3.1", "HALF");
        ITEM_SCALE.put("3.2", "HALF");
        ITEM_SCALE.put("4.0", "FULL");
        ITEM_SCALE.put("5.0", "FULL");
        ITEM_SCALE.put("6.1", "THIRD");
        ITEM_SCALE.put("6.2", "THIRD");
        ITEM_SCALE.put("6.3", "THIRD_66");
        ITEM_SCALE.put("7.0", "FULL");
        ITEM_SCALE.put("8.0", "FULL");
        ITEM_SCALE.put("9.1", "HALF");
        ITEM_SCALE.put("9.2", "HALF");
        ITEM_SCALE.put("10.1", "THIRD");
        ITEM_SCALE.put("10.2", "THIRD");
        ITEM_SCALE.put("10.3", "THIRD");
    }

    /** The items of every criterion, in document order. */
    private static final Map<String, List<String>> CRITERIA = new LinkedHashMap<>();

    static {
        CRITERIA.put("1.0", List.of("1.1", "1.2"));
        CRITERIA.put("2.0", List.of("2.0"));
        CRITERIA.put("3.0", List.of("3.1", "3.2"));
        CRITERIA.put("4.0", List.of("4.0"));
        CRITERIA.put("5.0", List.of("5.0"));
        CRITERIA.put("6.0", List.of("6.1", "6.2", "6.3"));
        CRITERIA.put("7.0", List.of("7.0"));
        CRITERIA.put("8.0", List.of("8.0"));
        CRITERIA.put("9.0", List.of("9.1", "9.2"));
        CRITERIA.put("10.0", List.of("10.1", "10.2", "10.3"));
    }

    /** Every criterion's stated maximum, in hundredths (2.00 for all ten). */
    private static final int CRITERION_MAX = 200;

    /** The theoretical maximum total, in hundredths (20.00). */
    private static final int MAX_TOTAL = 2000;

    private static boolean isValidResponse(String response) {
        return "NO".equals(response) || "PARTLY".equals(response) || "YES".equals(response);
    }

    /** The hundredths score of one item, or -1 when it is unanswered or invalid. */
    private static int itemScore(String itemKey, String response) {
        String scaleKey = ITEM_SCALE.get(itemKey);
        if (scaleKey == null || !isValidResponse(response)) {
            return -1;
        }
        return SCALES.get(scaleKey).get(response);
    }

    /**
     * Recomputes and stores the total score, the interpretation and the completion state of a
     * checklist from its responses. Called on every save, so a score sent by a client is
     * always overwritten by the value the official rules produce.
     */
    public static void recalculate(GadChecklistData checklist) {
        if (checklist == null) {
            return;
        }
        Map<String, String> responses = checklist.getResponses();
        int total = 0;
        boolean complete = true;
        for (List<String> items : CRITERIA.values()) {
            int subtotal = 0;
            for (String itemKey : items) {
                String response = responses == null ? null : responses.get(itemKey);
                int score = itemScore(itemKey, response);
                if (score < 0) {
                    complete = false;
                } else {
                    subtotal += score;
                }
            }
            total += Math.min(subtotal, CRITERION_MAX);
        }
        total = Math.min(total, MAX_TOTAL);
        checklist.setTotalScore(BigDecimal.valueOf(total, 2));
        checklist.setCompleted(complete);
        checklist.setInterpretation(complete ? interpretation(total) : "");
    }

    /** True when every one of the 17 assessment items has a valid response. */
    public static boolean isComplete(GadChecklistData checklist) {
        if (checklist == null || checklist.getResponses() == null) {
            return false;
        }
        for (String itemKey : ITEM_SCALE.keySet()) {
            if (!isValidResponse(checklist.getResponses().get(itemKey))) {
                return false;
            }
        }
        return true;
    }

    /** The official interpretation of a total score (hundredths). */
    private static String interpretation(int hundredthsTotal) {
        if (hundredthsTotal <= 399) {
            return "GAD is invisible in the project (proposal is returned).";
        }
        if (hundredthsTotal <= 799) {
            return "Proposed project has promising GAD prospects";
        }
        if (hundredthsTotal <= 1499) {
            return "Proposed project is gender-sensitive";
        }
        return "Proposed project is gender-responsive";
    }
}
