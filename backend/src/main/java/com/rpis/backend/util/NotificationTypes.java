package com.rpis.backend.util;

import java.util.Locale;
import java.util.Set;

/**
 * The canonical notification type vocabulary.
 *
 * <p>Types are stored as free text in {@code notifications.type}, so every producer and
 * consumer must agree on the exact spelling. Keeping the strings in one place is what
 * stops the two meanings the old {@code ENDORSEMENT} value carried from creeping back:
 * a Dean endorsement request is {@link #ENDORSEMENT}, while a proposal the RPS has
 * endorsed onward to OVCRIGE is {@link #PROPOSAL_ENDORSED}.
 *
 * <p>Historical rows written before {@link #PROPOSAL_ENDORSED} existed still carry
 * {@code ENDORSEMENT} for the proposal-forwarding events. The frontend resolver treats
 * those rows by recipient role, so no data rewrite is required.
 */
public final class NotificationTypes {

    private NotificationTypes() {
        // Constants only.
    }

    /** A proposal-workflow update addressed to ORPS or a reviewing office. */
    public static final String PROPOSAL_UPDATE = "PROPOSAL_UPDATE";

    /** The REC is asked to evaluate a proposal. */
    public static final String REVIEW_REQUEST = "REVIEW_REQUEST";

    /** An approval or a request for final approval. */
    public static final String APPROVAL = "APPROVAL";

    /** A proposal was returned for revision. */
    public static final String REVISION = "REVISION";

    /** A proposal was rejected. */
    public static final String REJECTION = "REJECTION";

    /** A budget / finance workflow action. */
    public static final String FUNDING = "FUNDING";

    /** A Dean endorsement request and its decisions. {@code proposalId} is the request id. */
    public static final String ENDORSEMENT = "ENDORSEMENT";

    /** The RPS endorsed a proposal to OVCRIGE. {@code proposalId} is the proposal id. */
    public static final String PROPOSAL_ENDORSED = "PROPOSAL_ENDORSED";

    /** A Call for Proposals publication or update. */
    public static final String PROPOSAL_ANNOUNCEMENT = "PROPOSAL_ANNOUNCEMENT";

    /** A quarterly progress or terminal report was submitted. */
    public static final String QUARTERLY_REPORT = "QUARTERLY_REPORT";

    /** Every required reporting stage was submitted and the project is complete. */
    public static final String PROJECT_COMPLETION = "PROJECT_COMPLETION";

    /** A Dean account was assigned, replaced or revoked. */
    public static final String DEAN_ASSIGNMENT = "DEAN_ASSIGNMENT";

    /** The OC saved a meaningful change to a Special Order draft. */
    public static final String SPECIAL_ORDER_DRAFT = "SPECIAL_ORDER_DRAFT";

    /** A Proponent's revision deadline is approaching. */
    public static final String REVISION_DEADLINE_REMINDER = "REVISION_DEADLINE_REMINDER";

    /** Reserved for a future evaluation deadline source; no producer exists today. */
    public static final String EVALUATION_DEADLINE_REMINDER = "EVALUATION_DEADLINE_REMINDER";

    /**
     * Types whose {@code proposalId} is a proposal id, not an endorsement request id.
     * Used by the read path when resolving the formatted Proposal ID and by consumers
     * that must not treat an endorsement request as a proposal.
     */
    public static final Set<String> PROPOSAL_WORKFLOW = Set.of(
            PROPOSAL_UPDATE,
            REVIEW_REQUEST,
            APPROVAL,
            REVISION,
            REJECTION,
            FUNDING,
            PROPOSAL_ENDORSED,
            QUARTERLY_REPORT,
            PROJECT_COMPLETION,
            SPECIAL_ORDER_DRAFT,
            REVISION_DEADLINE_REMINDER,
            EVALUATION_DEADLINE_REMINDER);

    /** True when the stored type belongs to the proposal workflow (case-insensitive). */
    public static boolean isProposalWorkflow(String type) {
        if (type == null) {
            return false;
        }
        return PROPOSAL_WORKFLOW.contains(type.trim().toUpperCase(Locale.ROOT));
    }
}
