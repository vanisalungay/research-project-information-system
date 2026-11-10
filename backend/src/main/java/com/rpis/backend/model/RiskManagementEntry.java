package com.rpis.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One row of the structured Risk Management Plan (section 21) of a proposal:
 * an identified risk, its likelihood, its impact and the mitigation measure
 * that addresses it.
 *
 * <p>Stored as a JSON array inside the {@code risk_management_json} column of
 * the parent {@link Proposal}, so several risk entries are persisted per
 * proposal with their four fields kept separate. It is not an entity of its own:
 * the rows only ever exist together with their proposal and are rewritten when
 * the plan is saved.</p>
 *
 * <p>{@code likelihood} and {@code impact} are constrained to
 * {@code High} / {@code Medium} / {@code Low} (see
 * {@code ProposalService#RISK_LEVELS}); every other value is rejected instead of
 * being stored.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskManagementEntry {

    /** The identified risk, e.g. "Delayed delivery of research equipment". */
    private String identifiedRisks;

    /** Likelihood of occurrence: High, Medium or Low. */
    private String likelihood;

    /** Potential impact: High, Medium or Low. */
    private String impact;

    /** The strategy or action that will address the identified risk. */
    private String mitigationMeasure;
}
