package com.rpis.backend.service;

import com.rpis.backend.model.User;
import com.rpis.backend.util.NotificationEvent;
import com.rpis.backend.util.NotificationPreferenceKeys;
import com.rpis.backend.util.NotificationTypes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The preference rules: defaults, explicit choices, legacy labels, mandatory workflow
 * alerts and the deadline opt-in.
 */
class NotificationPreferencePolicyTest {

    private final NotificationPreferencePolicy policy = new NotificationPreferencePolicy();

    private User user(boolean configured, List<String> proposals, List<String> deadlines) {
        User user = new User();
        user.setId(1L);
        user.setNotifyPreferencesConfigured(configured);
        user.setNotifyProposals(proposals);
        user.setNotifyDeadlines(deadlines);
        return user;
    }

    @Test
    void aNeverConfiguredAccountKeepsOptionalProposalAlertsButHasNoDeadlineReminders() {
        User user = user(false, List.of(), List.of());

        assertTrue(policy.shouldNotify(user, NotificationTypes.PROPOSAL_UPDATE,
                NotificationEvent.NEW_PROPOSAL));
        assertTrue(policy.shouldNotify(user, NotificationTypes.PROPOSAL_UPDATE,
                NotificationEvent.STATUS_UPDATED));
        assertFalse(policy.shouldNotify(user, NotificationTypes.REVISION_DEADLINE_REMINDER,
                NotificationEvent.REVISION_DEADLINE_REMINDER));
    }

    @Test
    void anExplicitAllOffChoiceSuppressesOptionalAlertsButNotCoreWorkflow() {
        User user = user(true, List.of(), List.of());

        assertFalse(policy.shouldNotify(user, NotificationTypes.PROPOSAL_UPDATE,
                NotificationEvent.NEW_PROPOSAL));
        assertFalse(policy.shouldNotify(user, NotificationTypes.PROPOSAL_ENDORSED,
                NotificationEvent.STATUS_UPDATED));
        assertTrue(policy.shouldNotify(user, NotificationTypes.REVIEW_REQUEST,
                NotificationEvent.NEW_PROPOSAL));
        assertTrue(policy.shouldNotify(user, NotificationTypes.REVISION,
                NotificationEvent.STATUS_UPDATED));
        assertTrue(policy.shouldNotify(user, NotificationTypes.REJECTION,
                NotificationEvent.STATUS_UPDATED));
        assertTrue(policy.shouldNotify(user, NotificationTypes.FUNDING,
                NotificationEvent.STATUS_UPDATED));
        assertTrue(policy.shouldNotify(user, NotificationTypes.ENDORSEMENT,
                NotificationEvent.STATUS_UPDATED));
    }

    @Test
    void aSelectedKeyEnablesOnlyThatOptionalGroup() {
        User user = user(true, List.of(NotificationPreferenceKeys.PROPOSAL_STATUS_UPDATED),
                List.of());

        assertTrue(policy.shouldNotify(user, NotificationTypes.PROPOSAL_UPDATE,
                NotificationEvent.STATUS_UPDATED));
        assertFalse(policy.shouldNotify(user, NotificationTypes.PROPOSAL_UPDATE,
                NotificationEvent.NEW_PROPOSAL));
        assertFalse(policy.shouldNotify(user, NotificationTypes.PROPOSAL_UPDATE,
                NotificationEvent.REVISION_SUBMITTED));
    }

    @Test
    void legacyDisplayLabelsAreRecognizedWithoutAReWrite() {
        User user = user(false,
                List.of("New Proposal Assigned", "Proposal Status Updated"), List.of());

        assertTrue(policy.shouldNotify(user, NotificationTypes.PROPOSAL_UPDATE,
                NotificationEvent.NEW_PROPOSAL));
        assertTrue(policy.shouldNotify(user, NotificationTypes.PROPOSAL_UPDATE,
                NotificationEvent.STATUS_UPDATED));
        assertFalse(policy.shouldNotify(user, NotificationTypes.PROPOSAL_UPDATE,
                NotificationEvent.REVISION_SUBMITTED),
                "an unlisted optional event stays off once a legacy list exists");
    }

    @Test
    void revisionDeadlineRemindersAreOptIn() {
        User configured = user(true, List.of(),
                List.of(NotificationPreferenceKeys.DEADLINE_REVISION));

        assertTrue(policy.shouldNotify(configured, NotificationTypes.REVISION_DEADLINE_REMINDER,
                NotificationEvent.REVISION_DEADLINE_REMINDER));

        User noDeadline = user(true, List.of(), List.of());
        assertFalse(policy.shouldNotify(noDeadline, NotificationTypes.REVISION_DEADLINE_REMINDER,
                NotificationEvent.REVISION_DEADLINE_REMINDER));
    }

    @Test
    void evaluationDeadlineRemindersHaveNoProducer() {
        User user = user(true, List.of(),
                List.of(NotificationPreferenceKeys.DEADLINE_EVALUATION));

        assertFalse(policy.shouldNotify(user, NotificationTypes.EVALUATION_DEADLINE_REMINDER,
                NotificationEvent.EVALUATION_DEADLINE_REMINDER));
    }

    @Test
    void aSoDraftUsesOnlyTheKeysForTheChangesThatHappened() {
        User statusOnly = user(true, List.of(NotificationPreferenceKeys.PROPOSAL_STATUS_UPDATED),
                List.of());
        assertTrue(policy.shouldNotify(statusOnly, NotificationTypes.SPECIAL_ORDER_DRAFT,
                NotificationEvent.soDraftChanged(true, false)));
        assertFalse(policy.shouldNotify(statusOnly, NotificationTypes.SPECIAL_ORDER_DRAFT,
                NotificationEvent.soDraftChanged(false, true)));

        User attachmentOnly = user(true,
                List.of(NotificationPreferenceKeys.PROPOSAL_ATTACHMENT_UPLOADED), List.of());
        assertFalse(policy.shouldNotify(attachmentOnly, NotificationTypes.SPECIAL_ORDER_DRAFT,
                NotificationEvent.soDraftChanged(true, false)));
        assertTrue(policy.shouldNotify(attachmentOnly, NotificationTypes.SPECIAL_ORDER_DRAFT,
                NotificationEvent.soDraftChanged(false, true)));
    }

    @Test
    void unknownTypesAndEventsFailOpen() {
        User user = user(true, List.of(), List.of());

        assertTrue(policy.shouldNotify(user, "FUTURE_TYPE", NotificationEvent.STATUS_UPDATED),
                "a new producer must not be silently hidden");
        assertTrue(policy.shouldNotify(user, NotificationTypes.PROPOSAL_UPDATE, null),
                "a producer that does not opt into preferences keeps its old behaviour");
    }
}
