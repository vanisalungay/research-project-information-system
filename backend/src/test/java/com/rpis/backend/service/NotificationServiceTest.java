package com.rpis.backend.service;

import com.rpis.backend.model.Notification;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.NotificationRepository;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the notification visibility rule: the RPS Admin manages accounts and takes no part
 * in the proposal review/processing workflow, so the notifications that workflow raises are
 * never returned to - and never marked read for - an RPS Admin. Every other role keeps them,
 * and the stored rows are never deleted or altered.
 *
 * <p>Plain Mockito unit test: no Spring context and no database, so it runs anywhere.
 */
class NotificationServiceTest {

    private static final Long ADMIN_ID = 1L;
    private static final Long STAFF_ID = 2L;

    /** Every type the proposal review/processing workflow raises. */
    private static final List<String> PROPOSAL_WORKFLOW_TYPES = List.of(
            "PROPOSAL_UPDATE", "REVIEW_REQUEST", "APPROVAL", "REVISION", "REJECTION", "FUNDING");

    private NotificationRepository notificationRepository;
    private UserRepository userRepository;
    private ProposalRepository proposalRepository;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        userRepository = mock(UserRepository.class);
        proposalRepository = mock(ProposalRepository.class);
        service = new NotificationService(notificationRepository, userRepository,
                mock(ProposalAnnouncementRepository.class), proposalRepository);
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

    /** One unread notification of every proposal-workflow type, in a stable order. */
    private List<Notification> workflowNotifications() {
        List<Notification> notifications = new ArrayList<>();
        long id = 10L;
        for (String type : PROPOSAL_WORKFLOW_TYPES) {
            notifications.add(notification(id++, type));
        }
        return notifications;
    }

    /** The inbox the service builds for an account whose stored rows are {@code rows}. */
    private List<Notification> inboxOf(User user, List<Notification> rows) {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(notificationRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(rows);
        return service.getNotificationsForUser(user.getId());
    }

    @Test
    void anRpsAdminNeverReceivesProposalWorkflowNotifications() {
        List<Notification> inbox = inboxOf(user(ADMIN_ID, "RPS_ADMIN"), workflowNotifications());

        assertTrue(inbox.isEmpty(),
                "an RPS Admin must not see any proposal review/processing notification, got " + inbox);
    }

    @Test
    void anRpsAdminStillReceivesCallsForProposalsAndAdministrativeOnes() {
        // What the role IS responsible for must keep arriving: a Call for Proposals
        // notification, an endorsement notification and an untyped administrative one.
        Notification call = notification(1L, "PROPOSAL_ANNOUNCEMENT");
        Notification endorsement = notification(2L, "ENDORSEMENT");
        Notification administrative = notification(3L, null);

        List<Notification> inbox = inboxOf(user(ADMIN_ID, "RPS_ADMIN"),
                List.of(call, endorsement, administrative));

        assertEquals(List.of(call, endorsement, administrative), inbox,
                "the notifications an RPS Admin is responsible for must still arrive, in order");
    }

    @Test
    void theExclusionAppliesToTheRpsAdminOnly() {
        for (String role : List.of("RPS_STAFF", "PROPONENT", "DEAN", "OVCRIGE", "REC", "OVCAF", "OC")) {
            List<Notification> rows = workflowNotifications();

            assertEquals(rows, inboxOf(user(STAFF_ID, role), rows),
                    role + " must keep every proposal notification");
        }
    }

    @Test
    void theTypeMatchIgnoresLetterCase() {
        // The stored type is compared case-insensitively, so a differently-cased row can
        // never slip past the rule.
        assertTrue(inboxOf(user(ADMIN_ID, "RPS_ADMIN"), List.of(notification(1L, "proposal_update")))
                .isEmpty());
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
    void markAllAsReadCannotTouchWhatTheAccountCannotSee() {
        User admin = user(ADMIN_ID, "RPS_ADMIN");
        Notification hidden = notification(1L, "PROPOSAL_UPDATE");
        Notification visible = notification(2L, "PROPOSAL_ANNOUNCEMENT");
        when(userRepository.findById(ADMIN_ID)).thenReturn(Optional.of(admin));
        when(notificationRepository.findByUserOrderByCreatedAtDesc(admin))
                .thenReturn(List.of(hidden, visible));

        service.markAllAsRead(ADMIN_ID);

        assertTrue(visible.getIsRead(), "a notification the account can see is marked read");
        assertFalse(hidden.getIsRead(), "a notification it cannot see must be left exactly as it was");
        verify(notificationRepository).saveAll(any());
    }

    @Test
    void markingAllAsReadStillCoversEveryNotificationOfAnotherRole() {
        User staff = user(STAFF_ID, "RPS_STAFF");
        Notification workflow = notification(1L, "REVIEW_REQUEST");
        Notification call = notification(2L, "PROPOSAL_ANNOUNCEMENT");
        when(userRepository.findById(STAFF_ID)).thenReturn(Optional.of(staff));
        when(notificationRepository.findByUserOrderByCreatedAtDesc(staff))
                .thenReturn(List.of(workflow, call));

        service.markAllAsRead(STAFF_ID);

        assertTrue(workflow.getIsRead(), "RPS Staff keeps and can clear its review notifications");
        assertTrue(call.getIsRead());
    }

    @Test
    void anRpsAdminCannotMarkAHiddenNotificationReadById() {
        // The ids are never disclosed to the account, but the write is refused anyway so the
        // rule cannot be bypassed with a hand-made request.
        User admin = user(ADMIN_ID, "RPS_ADMIN");
        Notification hidden = notification(1L, "APPROVAL");
        hidden.setUser(admin);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(hidden));

        Notification returned = service.markAsReadForCurrentUser(1L, ADMIN_ID, false);

        assertFalse(returned.getIsRead(), "the row must be returned untouched");
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void anotherRoleCanStillMarkItsOwnProposalNotificationRead() {
        User staff = user(STAFF_ID, "RPS_STAFF");
        Notification own = notification(1L, "REVIEW_REQUEST");
        own.setUser(staff);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(own));
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertTrue(service.markAsReadForCurrentUser(1L, STAFF_ID, true).getIsRead(),
                "RPS Staff must still be able to clear its own review notification");
    }
}
