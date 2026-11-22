package com.rpis.backend.util;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Stable keys for the two notification-preference lists.
 *
 * <p>The Profile Settings page historically stored the visible labels themselves. Labels
 * are not safe identifiers: editing the copy would silently stop matching. New saves use
 * these keys, while {@link #normalize(Collection)} maps the legacy labels so existing
 * accounts keep working without a database rewrite.
 */
public final class NotificationPreferenceKeys {

    private NotificationPreferenceKeys() {
    }

    public static final String PROPOSAL_NEW_ASSIGNED = "proposal.new_assigned";
    public static final String PROPOSAL_REVISION_SUBMITTED = "proposal.revision_submitted";
    public static final String PROPOSAL_ATTACHMENT_UPLOADED = "proposal.attachment_uploaded";
    public static final String PROPOSAL_STATUS_UPDATED = "proposal.status_updated";
    public static final String DEADLINE_EVALUATION = "deadline.evaluation";
    public static final String DEADLINE_REVISION = "deadline.revision";

    /** Legacy display label -> stable key. */
    private static final Map<String, String> LEGACY_LABELS = Map.of(
            "New Proposal Assigned", PROPOSAL_NEW_ASSIGNED,
            "Proponent Submitted Revision", PROPOSAL_REVISION_SUBMITTED,
            "New Attachment Uploaded", PROPOSAL_ATTACHMENT_UPLOADED,
            "Proposal Status Updated", PROPOSAL_STATUS_UPDATED,
            "Evaluation Deadline Reminders", DEADLINE_EVALUATION,
            "Revision Deadline Reminders", DEADLINE_REVISION);

    /** Normalizes labels/keys, trims, drops blanks, de-duplicates and preserves unknowns. */
    public static Set<String> normalize(Collection<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        if (values == null) {
            return normalized;
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            String trimmed = value.trim();
            normalized.add(LEGACY_LABELS.getOrDefault(trimmed, trimmed));
        }
        return normalized;
    }

    public static boolean selected(Collection<String> values, String key) {
        return normalize(values).contains(key);
    }

    /** True when the stored list contains a label that predates stable keys. */
    public static boolean looksLikeLegacyLabels(Collection<String> values) {
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && LEGACY_LABELS.containsKey(value.trim())) {
                return true;
            }
        }
        return false;
    }

    /** Case-normalized key, used only for defensive comparisons. */
    public static String canonical(String key) {
        return key == null ? null : key.trim().toLowerCase(Locale.ROOT);
    }
}
