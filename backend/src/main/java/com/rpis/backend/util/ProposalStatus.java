package com.rpis.backend.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

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
