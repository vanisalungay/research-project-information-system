package com.rpis.backend.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The workflow status of a proposal once the Proponent submits it to ORPS.
 * <p>
 * A submission is stored as {@link #FOR_REVIEW} ("FOR REVIEW", with a space - never an
 * underscore). Proposals saved before that wording change carry {@link #LEGACY_SUBMITTED};
 * the two values mean the exact same workflow state and are treated as equivalent by every
 * filter, notification and version-history check.
 * <p>
 * The frontend still sends {@code SUBMITTED} as the submission <em>action</em>; the backend
 * translates it into the stored status in {@code ProposalService.saveProposal}.
 * <p>
 * Kept as a dependency-free helper (no Spring bean) so {@code ProposalService} and
 * {@code ProposalVersionService} can share the rule, mirroring
 * {@link AnnouncementStatus}. The constants are compile-time constants, so they can also be
 * used as {@code switch} case labels.
 */
public final class ProposalStatus {

    public static final String DRAFT = "DRAFT";

    /** Stored status of a proposal the Proponent has submitted and ORPS must review. */
    public static final String FOR_REVIEW = "FOR REVIEW";

    /** Legacy value stored before the rename; the same workflow state as {@link #FOR_REVIEW}. */
    public static final String LEGACY_SUBMITTED = "SUBMITTED";

    /**
     * Every status a proposal or a proposal status history row may store.
     *
     * <p>Mirrors the codes seeded into {@code public.proposal_statuses} by migration-03,
     * plus {@link #FOR_REVIEW}, which is the current application spelling and is added to
     * that lookup by {@code migration-norm-04-proposal-status-fks.sql}. The legacy
     * {@link #LEGACY_SUBMITTED} spelling is accepted as input but is always persisted as
     * {@link #FOR_REVIEW}, so it is intentionally not a member of the stored set.</p>
     */
    public static final Set<String> SUPPORTED_STORED_STATUSES = Set.of(
            DRAFT,
            FOR_REVIEW,
            "RPS_RETURNED",
            "RETURNED_TO_RPS",
            "ENDORSED",
            "OVCRIGE_COORDINATION",
            "UNDER_REVIEW",
            "REC_EVALUATION",
            "REC_APPROVED",
            "OVC_APPROVED",
            "CHANCELLOR_REVIEW",
            "FOR_OC_APPROVAL",
            "FOR_OVCAF_APPROVAL",
            "FINANCE_AUTHORIZATION",
            "READY_FOR_RELEASE",
            "RELEASED",
            "IMPLEMENTATION",
            "APPROVED",
            "APPROVED_ENDORSED",
            "COMPLETED",
            "REJECTED",
            "REC_REJECTED",
            "REVISION",
            "REC_REVISION",
            "PENDING_REVISION",
            "RETURNED",
            "RETURNED_FOR_REVISION",
            "RPS_REVIEW");

    private ProposalStatus() {
    }

    /** Trimmed, upper-cased status; {@code null} when the value is missing or blank. */
    public static String normalize(String status) {
        if (status == null) {
            return null;
        }
        String value = status.trim().toUpperCase(Locale.ROOT);
        return value.isEmpty() ? null : value;
    }

    /** True for both spellings of the submitted/for-review state. */
    public static boolean isForReview(String status) {
        String value = normalize(status);
        return FOR_REVIEW.equals(value) || LEGACY_SUBMITTED.equals(value);
    }

    /** True when the request is a submission (either spelling) rather than a draft save. */
    public static boolean isSubmissionRequest(String status) {
        return isForReview(status);
    }

    /**
     * The status actually persisted for a requested status: both submission spellings are
     * stored as {@link #FOR_REVIEW}; every other status keeps the previous upper-case
     * behaviour.
     */
    public static String toStoredStatus(String requested) {
        String value = normalize(requested);
        if (value == null) {
            return null;
        }
        return isForReview(value) ? FOR_REVIEW : value;
    }

    /**
     * The canonical status to persist, rejecting a missing or unknown value.
     *
     * <p>This is the application-level guard in front of the proposal-status foreign
     * keys: an API or integration value that is not part of the RPIS workflow fails with
     * a readable {@link IllegalArgumentException} (HTTP 400) instead of reaching
     * PostgreSQL and surfacing as a foreign-key violation. The legacy {@code SUBMITTED}
     * input is still accepted and converted to {@link #FOR_REVIEW}.</p>
     *
     * @throws IllegalArgumentException when the status is null, blank or not supported
     */
    public static String requireSupportedStatus(String status) {
        String stored = toStoredStatus(status);
        if (stored == null) {
            throw new IllegalArgumentException("A proposal status is required.");
        }
        if (!SUPPORTED_STORED_STATUSES.contains(stored)) {
            throw new IllegalArgumentException(
                    "Invalid proposal status '" + normalize(status)
                            + "'. Use a status supported by the proposal workflow.");
        }
        return stored;
    }

    /**
     * Same as {@link #requireSupportedStatus(String)} but treats a missing or blank value
     * as "not provided" and returns {@code null}. Used for optional history values such as
     * {@code from_status} on a baseline row.
     */
    public static String optionalSupportedStatus(String status) {
        return normalize(status) == null ? null : requireSupportedStatus(status);
    }

    /**
     * Expands an exact status filter so a filter for either submission spelling also
     * matches the other one. Inputs and outputs are normalized, deduplicated and kept in
     * encounter order; blank entries are dropped.
     */
    public static List<String> withForReviewAliases(List<String> statuses) {
        LinkedHashSet<String> expanded = new LinkedHashSet<>();
        if (statuses != null) {
            for (String status : statuses) {
                String value = normalize(status);
                if (value == null) {
                    continue;
                }
                expanded.add(value);
                if (isForReview(value)) {
                    expanded.add(FOR_REVIEW);
                    expanded.add(LEGACY_SUBMITTED);
                }
            }
        }
        return new ArrayList<>(expanded);
    }
}
