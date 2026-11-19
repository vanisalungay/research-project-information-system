package com.rpis.backend.util;

import java.util.Set;

/**
 * What a notification represents for preference purposes.
 *
 * <p>A governed event is optional and is filtered by the account's saved preferences.
 * An ungoverned event ({@link #NONE}) is outside the two preference groups and is always
 * delivered. The notification type still decides whether an event is mandatory: core
 * workflow alerts bypass the optional checkboxes even when their event is governed.
 */
public record NotificationEvent(
        String name,
        boolean governed,
        Set<String> proposalKeys,
        Set<String> deadlineKeys,
        boolean deadlineOptIn,
        String dedupeKey) {

    public NotificationEvent {
        proposalKeys = proposalKeys == null ? Set.of() : Set.copyOf(proposalKeys);
        deadlineKeys = deadlineKeys == null ? Set.of() : Set.copyOf(deadlineKeys);
    }

    public static final NotificationEvent NONE =
            new NotificationEvent("none", false, Set.of(), Set.of(), false, null);

    public static final NotificationEvent NEW_PROPOSAL = new NotificationEvent(
            "new_proposal", true,
            Set.of(NotificationPreferenceKeys.PROPOSAL_NEW_ASSIGNED),
            Set.of(), false, null);

    public static final NotificationEvent REVISION_SUBMITTED = new NotificationEvent(
            "revision_submitted", true,
            Set.of(NotificationPreferenceKeys.PROPOSAL_REVISION_SUBMITTED),
            Set.of(), false, null);

    public static final NotificationEvent STATUS_UPDATED = new NotificationEvent(
            "status_updated", true,
            Set.of(NotificationPreferenceKeys.PROPOSAL_STATUS_UPDATED),
            Set.of(), false, null);

    public static final NotificationEvent ATTACHMENT_UPLOADED = new NotificationEvent(
            "attachment_uploaded", true,
            Set.of(NotificationPreferenceKeys.PROPOSAL_ATTACHMENT_UPLOADED),
            Set.of(), false, null);

    /** The SO draft event carries the keys corresponding to the changes that occurred. */
    public static NotificationEvent soDraftChanged(boolean statusChanged, boolean attachmentChanged) {
        Set<String> keys = new java.util.LinkedHashSet<>();
        if (statusChanged) {
            keys.add(NotificationPreferenceKeys.PROPOSAL_STATUS_UPDATED);
        }
        if (attachmentChanged) {
            keys.add(NotificationPreferenceKeys.PROPOSAL_ATTACHMENT_UPLOADED);
        }
        return new NotificationEvent("so_draft_changed", true, keys, Set.of(), false, null);
    }

    public static final NotificationEvent REVISION_DEADLINE_REMINDER = new NotificationEvent(
            "revision_deadline_reminder", true,
            Set.of(),
            Set.of(NotificationPreferenceKeys.DEADLINE_REVISION),
            true, null);

    public static final NotificationEvent EVALUATION_DEADLINE_REMINDER = new NotificationEvent(
            "evaluation_deadline_reminder", true,
            Set.of(),
            Set.of(NotificationPreferenceKeys.DEADLINE_EVALUATION),
            true, null);

    /** Same event with an idempotency key used by generated notifications. */
    public NotificationEvent withDedupeKey(String key) {
        return new NotificationEvent(name, governed, proposalKeys, deadlineKeys,
                deadlineOptIn, key);
    }
}
