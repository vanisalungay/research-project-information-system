package com.rpis.backend.service;

import com.rpis.backend.model.User;
import com.rpis.backend.util.NotificationEvent;
import com.rpis.backend.util.NotificationPreferenceKeys;
import com.rpis.backend.util.NotificationTypes;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

/**
 * Decides whether a notification should be written for one recipient.
 *
 * <p>Rules:
 * <ul>
 *   <li>Core workflow alerts are always delivered: review requests, returns,
 *       rejections, approvals, financial actions and endorsement requests.</li>
 *   <li>Types outside the two preference groups are always delivered.</li>
 *   <li>An account that has not saved the Notifications tab gets optional proposal
 *       alerts by default, but no deadline reminders (opt-in).</li>
 *   <li>An account that saved the tab gets exactly the optional keys it selected; an
 *       empty selection is a real all-off choice.</li>
 *   <li>Unknown types fail open, so a future producer cannot be silently hidden.</li>
 * </ul>
 */
@Service
public class NotificationPreferencePolicy {

    /** Types the workflow cannot advance without; optional checkboxes never suppress them. */
    private static final Set<String> MANDATORY_TYPES = Set.of(
            NotificationTypes.REVIEW_REQUEST,
            NotificationTypes.REVISION,
            NotificationTypes.REJECTION,
            NotificationTypes.APPROVAL,
            NotificationTypes.FUNDING,
            NotificationTypes.ENDORSEMENT);

    /**
     * The only types that can be suppressed by a preference. Everything else - the
     * mandatory set above, the types outside the preference groups, and any future type -
     * is delivered, so a new producer is never silently hidden.
     */
    private static final Set<String> OPTIONAL_TYPES = Set.of(
            NotificationTypes.PROPOSAL_UPDATE,
            NotificationTypes.PROPOSAL_ENDORSED,
            NotificationTypes.SPECIAL_ORDER_DRAFT,
            NotificationTypes.REVISION_DEADLINE_REMINDER,
            NotificationTypes.EVALUATION_DEADLINE_REMINDER);

    public boolean shouldNotify(User user, String type, NotificationEvent event) {
        if (user == null || event == null || !event.governed()) {
            return true;
        }
        if (type == null) {
            return true;
        }
        String normalizedType = type.trim().toUpperCase(Locale.ROOT);
        if (MANDATORY_TYPES.contains(normalizedType)
                || !OPTIONAL_TYPES.contains(normalizedType)) {
            return true;
        }

        // No evaluation deadline data exists, so that preference stays inert rather than
        // pretending an alert could be generated.
        if (event.deadlineKeys().contains(NotificationPreferenceKeys.DEADLINE_EVALUATION)) {
            return false;
        }

        boolean configured = Boolean.TRUE.equals(user.getNotifyPreferencesConfigured());

        if (!event.deadlineKeys().isEmpty()) {
            Set<String> selected = NotificationPreferenceKeys.normalize(user.getNotifyDeadlines());
            if (configured || !selected.isEmpty()) {
                return intersects(selected, event.deadlineKeys());
            }
            // Never configured: deadline reminders are opt-in.
            return !event.deadlineOptIn();
        }

        if (!event.proposalKeys().isEmpty()) {
            Set<String> selected = NotificationPreferenceKeys.normalize(user.getNotifyProposals());
            if (configured || !selected.isEmpty()) {
                return intersects(selected, event.proposalKeys());
            }
            // Never configured: informational proposal alerts stay on, preserving the
            // behaviour every existing account already has.
            return true;
        }

        return true;
    }

    private static boolean intersects(Collection<String> selected, Collection<String> required) {
        for (String key : required) {
            if (selected.contains(key)) {
                return true;
            }
        }
        return false;
    }
}
