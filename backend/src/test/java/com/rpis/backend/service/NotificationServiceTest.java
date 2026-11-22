package com.rpis.backend.service;

import com.rpis.backend.model.Notification;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.NotificationRepository;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.util.NotificationEvent;
import com.rpis.backend.util.NotificationPreferenceKeys;
import com.rpis.backend.util.NotificationTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain Mockito unit test for the notification read path and recipient helpers: no Spring
 * context and no database.
 *
 * <p>The RPS Administrator is a participant in the office's work, so proposal-workflow
 * rows addressed to that account are visible like every other account's rows.
 */
class NotificationServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long STAFF_ID = 2L;

    private NotificationRepository notificationRepository;
    private UserRepository userRepository;
    private ProposalRepository proposalRepository;
    private ApplicationEventPublisher eventPublisher;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        userRepository = mock(UserRepository.class);
        proposalRepository = mock(ProposalRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new NotificationService(notificationRepository, userRepository,
                mock(ProposalAnnouncementRepository.class), proposalRepository, eventPublisher,
                new NotificationPreferencePolicy());
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private User user(Long id, String role) {
        User user = new User();
        user.setId(id);
        user.setName(role + " account");
        user.setRole(role);
        return user;
    }

    private Notification notification(Long id, String type) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setType(type);
        notification.setMessage("message " + id);
        notification.setIsRead(false);
        return notification;
    }

    private Proposal proposal(Long id, String documentId) {
        Proposal proposal = new Proposal();
        proposal.setId(id);
        proposal.setDocumentId(documentId);
        return proposal;
    }

    private List<Notification> inboxOf(User user, List<Notification> rows) {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(notificationRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(rows);
        return service.getNotificationsForUser(user.getId());
    }

    @Test
    void anRpsAdminReceivesProposalWorkflowNotifications() {
        List<Notification> rows = new ArrayList<>();
        long id = 10L;
        for (String type : List.of("PROPOSAL_UPDATE", "REVIEW_REQUEST", "APPROVAL",
                "REVISION", "REJECTION", "FUNDING", "PROPOSAL_ENDORSED")) {
            rows.add(notification(id++, type));
        }

        List<Notification> inbox = inboxOf(user(ADMIN_ID, "RPS_ADMIN"), rows);

        assertEquals(rows, inbox,
                "the administrator participates in the office workflow and must see its notifications");
    }

    @Test
    void theFormattedProposalIdComesFromTheProposalRecord() {
        User staff = user(STAFF_ID, "RPS_STAFF");
        Notification initial = notification(1L, "PROPOSAL_UPDATE");
        initial.setProposalId(84L);
        Notification revision = notification(2L, "REVISION");
        revision.setProposalId(85L);
        Notification endorsement = notification(3L, "ENDORSEMENT");
        endorsement.setProposalId(84L); // an endorsement request id, not a proposal id
        Notification missingProposal = notification(4L, "APPROVAL");
        missingProposal.setProposalId(999L);
        Notification noProposal = notification(5L, "APPROVAL");

        when(proposalRepository.findAllById(any())).thenReturn(List.of(
                proposal(84L, "MSUN-ORPS-DA-123-2026-REV00"),
                proposal(85L, "MSUN-ORPS-DA-124-2026-REV02")));

        inboxOf(staff, List.of(initial, revision, endorsement, missingProposal, noProposal));

        assertEquals("MSUN-ORPS-DA-123-2026-REV00", initial.getProposalDocumentId(),
                "the displayed id is the stored formatted id, not the database primary key");
        assertEquals("MSUN-ORPS-DA-124-2026-REV02", revision.getProposalDocumentId(),
                "the revision suffix is kept exactly as recorded");
        assertNull(endorsement.getProposalDocumentId(),
                "an endorsement's proposalId is an endorsement request id and must not be resolved");
        assertNull(missingProposal.getProposalDocumentId(),
                "a missing proposal yields no identifier rather than a wrong one");
        assertNull(noProposal.getProposalDocumentId());
        verify(proposalRepository).findAllById(Set.of(84L, 85L, 999L));
    }

    @Test
    void markAllAsReadMarksEveryNotificationOfTheAccount() {
        User admin = user(ADMIN_ID, "RPS_ADMIN");
        Notification workflow = notification(1L, "PROPOSAL_UPDATE");
        Notification call = notification(2L, "PROPOSAL_ANNOUNCEMENT");
        workflow.setUser(admin);
        call.setUser(admin);
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(notificationRepository.findByUserOrderByCreatedAtDesc(admin))
                .thenReturn(List.of(workflow, call));

        service.markAllAsRead(ADMIN_ID);

        assertTrue(workflow.getIsRead());
        assertTrue(call.getIsRead());
        verify(notificationRepository).saveAll(anyList());
        verify(eventPublisher).publishEvent(new NotificationChangedEvent(ADMIN_ID, "read-all"));
    }

    @Test
    void anAccountCannotMarkAnotherAccountsNotificationRead() {
        User staff = user(STAFF_ID, "RPS_STAFF");
        User other = user(ADMIN_ID, "RPS_ADMIN");
        Notification foreign = notification(1L, "APPROVAL");
        foreign.setUser(other);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(foreign));

        assertThrows(ResponseStatusException.class,
                () -> service.markAsReadForCurrentUser(1L, STAFF_ID),
                "ownership is enforced for every role, not only Proponents");
        assertFalse(foreign.getIsRead());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void theOwnerCanMarkTheirOwnNotificationRead() {
        User staff = user(STAFF_ID, "RPS_STAFF");
        Notification own = notification(1L, "REVIEW_REQUEST");
        own.setUser(staff);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(own));

        assertTrue(service.markAsReadForCurrentUser(1L, STAFF_ID).getIsRead());
        verify(eventPublisher).publishEvent(new NotificationChangedEvent(STAFF_ID, "read"));
    }

    @Test
    void notifyUsersWritesOneRowPerDistinctAccount() {
        User first = user(1L, "RPS_ADMIN");
        User duplicate = user(1L, "RPS_ADMIN");
        User second = user(2L, "RPS_STAFF");

        List<Notification> created = service.notifyUsers(
                List.of(first, duplicate, second),
                "message", "title", "APPROVAL", 7L);

        assertEquals(2, created.size(), "a duplicate recipient id must not create a second row");
        verify(notificationRepository, times(2)).save(any(Notification.class));
        verify(eventPublisher, times(2)).publishEvent(any(NotificationCreatedEvent.class));
    }

    @Test
    void anExplicitAllOffChoiceSuppressesOptionalProposalAlerts() {
        User staff = user(STAFF_ID, "RPS_STAFF");
        staff.setNotifyPreferencesConfigured(true);
        staff.setNotifyProposals(List.of());

        List<Notification> created = service.notifyUsers(
                List.of(staff), "message", "title", NotificationTypes.PROPOSAL_UPDATE,
                7L, NotificationEvent.NEW_PROPOSAL);

        assertTrue(created.isEmpty());
        verify(notificationRepository, never()).save(any(Notification.class));
        verify(eventPublisher, never()).publishEvent(any(NotificationCreatedEvent.class));
    }

    @Test
    void aMandatoryWorkflowAlertIsWrittenEvenWhenOptionalAlertsAreOff() {
        User staff = user(STAFF_ID, "RPS_STAFF");
        staff.setNotifyPreferencesConfigured(true);
        staff.setNotifyProposals(List.of());

        List<Notification> created = service.notifyUsers(
                List.of(staff), "message", "title", NotificationTypes.REVIEW_REQUEST,
                7L, NotificationEvent.NEW_PROPOSAL);

        assertEquals(1, created.size());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void aSelectionThatMatchesTheEventKeepsTheOptionalAlert() {
        User staff = user(STAFF_ID, "RPS_STAFF");
        staff.setNotifyPreferencesConfigured(true);
        staff.setNotifyProposals(List.of(NotificationPreferenceKeys.PROPOSAL_STATUS_UPDATED));

        List<Notification> created = service.notifyUsers(
                List.of(staff), "message", "title", NotificationTypes.PROPOSAL_UPDATE,
                7L, NotificationEvent.STATUS_UPDATED);

        assertEquals(1, created.size());
    }

    @Test
    void anExistingDedupeKeySuppressesTheGeneratedRow() {
        User staff = user(STAFF_ID, "RPS_STAFF");
        staff.setNotifyPreferencesConfigured(true);
        staff.setNotifyDeadlines(List.of(NotificationPreferenceKeys.DEADLINE_REVISION));
        when(notificationRepository.existsByUserIdAndDedupeKey(STAFF_ID, "reminder-key"))
                .thenReturn(true);

        List<Notification> created = service.notifyUsers(
                List.of(staff), "message", "title", NotificationTypes.REVISION_DEADLINE_REMINDER,
                7L, NotificationEvent.REVISION_DEADLINE_REMINDER.withDedupeKey("reminder-key"));

        assertTrue(created.isEmpty());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void notifyRolesResolvesRolesWithOneQuery() {
        // The recipient query includes the role-assignment table, so a multi-role account
        // is found by every role it holds.
        when(userRepository.findByAnyAssignedRoleIn(anyList()))
                .thenReturn(List.of(user(1L, "RPS_ADMIN"), user(2L, "RPS_STAFF")));

        List<Notification> created = service.notifyRoles(
                List.of("RPS_ADMIN", "RPS_STAFF"), "message", "title", "REVISION", 9L);

        assertEquals(2, created.size());
        assertEquals("RPS_ADMIN", created.get(0).getRecipientRole());
        assertEquals("RPS_STAFF", created.get(1).getRecipientRole());
        verify(userRepository).findByAnyAssignedRoleIn(anyList());
    }

    @Test
    void aMultiRoleRecipientGetsOneRowTaggedWithTheRequestedRole() {
        User multi = new User();
        multi.setId(7L);
        multi.setName("Proponent who is also Dean");
        multi.setRole("PROPONENT");
        multi.assignRole("DEAN");
        when(userRepository.findByAnyAssignedRoleIn(anyList())).thenReturn(List.of(multi));

        List<Notification> created = service.notifyRoles(
                List.of("DEAN", "PROPONENT"), "message", "title", "ENDORSEMENT", 11L);

        assertEquals(1, created.size(), "one recipient, one row");
        assertEquals("DEAN", created.get(0).getRecipientRole(),
                "the row belongs to the first requested role the account holds");
    }
}
