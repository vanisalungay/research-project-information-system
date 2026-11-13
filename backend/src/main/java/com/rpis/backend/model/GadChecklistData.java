package com.rpis.backend.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Section 19 - the interactive GAD Checklist 2 (Project Identification and Design Stages),
 * stored as JSON inside the {@code gad_checklist_json} column of {@link Proposal}.
 *
 * <p>{@code responses} and {@code comments} are keyed by the assessment item id ("1.1" ...
 * "10.3"). The {@code totalScore}, {@code interpretation} and {@code completed} values are
 * DERIVED: the backend always recalculates them from the responses (see
 * {@code GadChecklistScoring}), so a client can never persist a wrong score.</p>
 */
@Data
@NoArgsConstructor
public class GadChecklistData {

    /** One response per assessment item: NO / PARTLY / YES. */
    private Map<String, String> responses = new LinkedHashMap<>();

    /** The proponent's notes / identified gender issues, one entry per assessment item. */
    private Map<String, String> comments = new LinkedHashMap<>();

    /** The total GAD score out of 20 (derived; never trusted from the client). */
    private BigDecimal totalScore;

    /** The official interpretation of the total (derived; empty while the checklist is incomplete). */
    private String interpretation;

    /** True once all 17 assessment items carry a response (derived). */
    private Boolean completed;
}
