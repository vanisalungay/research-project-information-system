package com.rpis.backend.service;

import com.rpis.backend.model.Notification;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.NotificationRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.util.NotificationEvent;
import com.rpis.backend.util.NotificationPreferenceKeys;
import com.rpis.backend.util.NotificationTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Preferences against the real JPA mapping and JSON-list converter.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationPreferencePostgresTest {

    @Autowired private NotificationService notificationService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setName("Preference Tester");
        user.setEmail("preference-" + System.nanoTime() + "@example.edu.ph");
        user.setPassword("secret");
        user.setRole("RPS_STAFF");
        user.setStatus("APPROVED");
        user = userRepository.saveAndFlush(user);
    }

    private List<Notification> notify(String type, NotificationEvent event) {
        return notificationService.notifyUsers(List.of(user), "message", "title", type,
                1L, event);
    }

    @Test
    void aNeverConfiguredAccountGetsOptionalProposalAlertsButNoDeadlineReminder() {
        assertEquals(1, notify(NotificationTypes.PROPOSAL_UPDATE,
                NotificationEvent.NEW_PROPOSAL).size());
        assertEquals(0, notify(NotificationTypes.REVISION_DEADLINE_REMINDER,
                NotificationEvent.REVISION_DEADLINE_REMINDER).size());
    }

    @Test
    void anExplicitAllOffChoiceKeepsMandatoryAlertsOnly() {
        user.setNotifyPreferencesConfigured(true);
        user.setNotifyProposals(List.of());
        user.setNotifyDeadlines(List.of());
        userRepository.saveAndFlush(user);

        assertEquals(0, notify(NotificationTypes.PROPOSAL_UPDATE,
                NotificationEvent.NEW_PROPOSAL).size());
        assertEquals(1, notify(NotificationTypes.REVIEW_REQUEST,
                NotificationEvent.NEW_PROPOSAL).size());
        assertEquals(1, notify(NotificationTypes.REJECTION,
                NotificationEvent.STATUS_UPDATED).size());
    }

    @Test
    void legacyLabelsAreNormalizedToKeys() {
        user.setNotifyProposals(List.of("Proposal Status Updated"));
        userRepository.saveAndFlush(user);

        assertEquals(1, notify(NotificationTypes.PROPOSAL_UPDATE,
                NotificationEvent.STATUS_UPDATED).size());
        assertEquals(0, notify(NotificationTypes.PROPOSAL_UPDATE,
                NotificationEvent.NEW_PROPOSAL).size());
    }

    @Test
    void savingTheNotificationsTabMarksTheAccountConfiguredAndStoresStableKeys() {
        User patch = new User();
        patch.setNotifyProposals(List.of("Proposal Status Updated"));
        patch.setNotifyDeadlines(List.of());

        User saved = userService.updateUser(user.getId(), patch);

        assertEquals(true, saved.getNotifyPreferencesConfigured());
        assertEquals(List.of(NotificationPreferenceKeys.PROPOSAL_STATUS_UPDATED),
                saved.getNotifyProposals());
        assertEquals(List.of(), saved.getNotifyDeadlines());
    }

    @Test
    void aSelectedRevisionDeadlineKeyEnablesTheReminder() {
        user.setNotifyPreferencesConfigured(true);
        user.setNotifyDeadlines(List.of(NotificationPreferenceKeys.DEADLINE_REVISION));
        userRepository.saveAndFlush(user);

        assertEquals(1, notify(NotificationTypes.REVISION_DEADLINE_REMINDER,
                NotificationEvent.REVISION_DEADLINE_REMINDER.withDedupeKey("deadline-1")).size());
        // The unique dedupe key suppresses the second attempt.
        assertEquals(0, notify(NotificationTypes.REVISION_DEADLINE_REMINDER,
                NotificationEvent.REVISION_DEADLINE_REMINDER.withDedupeKey("deadline-1")).size());

        assertEquals(1, notificationRepository.findByUserOrderByCreatedAtDesc(user).size());
    }
}
