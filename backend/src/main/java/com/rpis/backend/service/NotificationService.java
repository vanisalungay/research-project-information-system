package com.rpis.backend.service;

import com.rpis.backend.model.Notification;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.NotificationRepository;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.util.AnnouncementStatus;
import com.rpis.backend.util.NotificationEvent;
import com.rpis.backend.util.NotificationTypes;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ProposalAnnouncementRepository announcementRepository;
    private final ProposalRepository proposalRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationPreferencePolicy preferencePolicy;

    public List<Notification> getNotificationsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        fillAnnouncementDetails(notifications);
        fillProposalDocumentIds(notifications);
        return notifications;
    }

    /**
     * Fills the computed (not persisted) formatted Proposal ID of every proposal-workflow
     * notification in one batch. The displayed identifier is the proposal's stored
     * {@code documentId} (for example {@code MSUN-ORPS-DA-028-2026-REV00}), never the
     * database primary key, so revision suffixes are kept exactly as recorded.
     * <p>
     * An endorsement notification stores the endorsement request id in {@code proposalId},
     * so only the proposal-workflow types are resolved here. A notification with no linked
     * proposal, or whose proposal no longer exists / has no formatted identifier, keeps the
     * field null and the UI shows no identifier rather than a wrong one.
     */
    private void fillProposalDocumentIds(List<Notification> notifications) {
        Set<Long> proposalIds = new HashSet<>();
        for (Notification notification : notifications) {
            if (NotificationTypes.isProposalWorkflow(notification.getType())
                    && notification.getProposalId() != null) {
                proposalIds.add(notification.getProposalId());
            }
        }
        if (proposalIds.isEmpty()) {
            return;
        }

        Map<Long, String> documentIds = proposalRepository.findAllById(proposalIds)
                .stream()
                .filter(proposal -> proposal.getDocumentId() != null
                        && !proposal.getDocumentId().isBlank())
                .collect(Collectors.toMap(Proposal::getId, Proposal::getDocumentId));

        for (Notification notification : notifications) {
            if (NotificationTypes.isProposalWorkflow(notification.getType())) {
                notification.setProposalDocumentId(
                        documentIds.get(notification.getProposalId()));
            }
        }
    }

    /**
     * Fills the computed (not persisted) Call for Proposals details a notification is
     * about: the call title and its <em>current</em> status (DRAFT, SCHEDULED, OPEN or
     * CLOSED). The status is derived at read time from the saved status plus the saved
     * dates - the same rule the Call for Proposals pages use - so a call that has since
     * opened reads as OPEN instead of the status it had when the notification was
     * raised.
     * <p>
     * Announcements are loaded in one batch for the whole page of notifications, so
     * listing them never turns into one query per notification. A notification without
     * an announcement (or whose announcement no longer exists) simply keeps both fields
     * null and shows no call details.
     */
    private void fillAnnouncementDetails(List<Notification> notifications) {
        Set<Long> announcementIds = new HashSet<>();
        for (Notification notification : notifications) {
            if (notification.getProposalAnnouncementId() != null) {
                announcementIds.add(notification.getProposalAnnouncementId());
            }
        }
        if (announcementIds.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now();
        Map<Long, ProposalAnnouncement> announcements = announcementRepository
                .findAllById(announcementIds)
                .stream()
                .collect(Collectors.toMap(ProposalAnnouncement::getId, Function.identity()));

        for (Notification notification : notifications) {
            ProposalAnnouncement announcement =
                    announcements.get(notification.getProposalAnnouncementId());
            if (announcement != null) {
                notification.setProposalAnnouncementName(announcement.getName());
                notification.setProposalAnnouncementStatus(
                        AnnouncementStatus.displayStatus(announcement, today));
            }
        }
    }

    public Notification createNotification(Long userId, String message) {
        return createNotification(userId, message, null, null, null);
    }

    public Notification createNotification(Long userId, String message, String title, String type, Long proposalId) {
        return createNotification(userId, message, title, type, proposalId, null);
    }

    /**
     * Creates an in-app notification that may also point at the Proposal Announcement
     * (Call for Proposals) it was raised for. {@code proposalAnnouncementId} is null for
     * every notification that is not about an announcement; the notification listing then
     * simply carries no announcement to open.
     */
    public Notification createNotification(Long userId, String message, String title, String type,
            Long proposalId, Long proposalAnnouncementId) {
        return createNotification(userId, message, title, type, proposalId,
                proposalAnnouncementId, null);
    }

    public Notification createNotification(Long userId, String message, String title, String type,
            Long proposalId, Long proposalAnnouncementId, NotificationEvent event) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return createNotificationForUser(user, message, title, type, proposalId,
                proposalAnnouncementId, event);
    }

    /**
     * Writes a notification for an account that has already been loaded.
     *
     * <p>Used when the recipients were fetched as a set (a status change notifies
     * every account in one or two roles). Without this overload each notification
     * re-read the same account by id, which turned one status change into a query per
     * recipient on top of the recipient lookup itself.
     */
    public Notification createNotificationForUser(User user, String message, String title,
            String type, Long proposalId) {
        return createNotificationForUser(user, message, title, type, proposalId, null);
    }

    public Notification createNotificationForUser(User user, String message, String title,
            String type, Long proposalId, Long proposalAnnouncementId) {
        return createNotificationForUser(user, message, title, type, proposalId,
                proposalAnnouncementId, null);
    }

    /**
     * Writes a notification unless the recipient's saved preferences suppress it or an
     * idempotency key has already produced it. Returning {@code null} means "nothing was
     * written"; callers that ignore the return value are unaffected.
     */
    public Notification createNotificationForUser(User user, String message, String title,
            String type, Long proposalId, Long proposalAnnouncementId, NotificationEvent event) {
        if (user == null) {
            throw new IllegalArgumentException("A recipient account is required.");
        }
        if (!preferencePolicy.shouldNotify(user, type, event)) {
            return null;
        }
        String dedupeKey = event == null ? null : event.dedupeKey();
        if (dedupeKey != null && user.getId() != null
                && notificationRepository.existsByUserIdAndDedupeKey(user.getId(), dedupeKey)) {
            return null;
        }
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setTitle(title != null ? title : "Notification");
        notification.setType(type);
        notification.setProposalId(proposalId);
        notification.setProposalAnnouncementId(proposalAnnouncementId);
        notification.setDedupeKey(dedupeKey);
        notification.setIsRead(false);
        Notification saved = notificationRepository.save(notification);
        // Delivered by NotificationRealtimePublisher only after the surrounding workflow
        // transaction commits. When there is no transaction the listener runs immediately.
        eventPublisher.publishEvent(new NotificationCreatedEvent(saved.getId(), user.getId()));
        return saved;
    }

    /**
     * Writes one notification to each distinct recipient. A user who appears twice in the
     * supplied collection (for example a proponent who is also returned by a role query)
     * receives exactly one row for the event. Null entries and accounts without an id are
     * ignored rather than failing the workflow.
     */
    public List<Notification> notifyUsers(Collection<User> recipients, String message, String title,
            String type, Long proposalId, Long proposalAnnouncementId) {
        return notifyUsers(recipients, message, title, type, proposalId,
                proposalAnnouncementId, null);
    }

    public List<Notification> notifyUsers(Collection<User> recipients, String message, String title,
            String type, Long proposalId, Long proposalAnnouncementId, NotificationEvent event) {
        if (recipients == null || recipients.isEmpty()) {
            return List.of();
        }
        Map<Long, User> unique = new LinkedHashMap<>();
        for (User user : recipients) {
            if (user != null && user.getId() != null) {
                unique.putIfAbsent(user.getId(), user);
            }
        }
        List<Notification> created = new ArrayList<>(unique.size());
        for (User user : unique.values()) {
            Notification notification = createNotificationForUser(
                    user, message, title, type, proposalId, proposalAnnouncementId, event);
            if (notification != null) {
                created.add(notification);
            }
        }
        return created;
    }

    public List<Notification> notifyUsers(Collection<User> recipients, String message, String title,
            String type, Long proposalId) {
        return notifyUsers(recipients, message, title, type, proposalId, null, null);
    }

    public List<Notification> notifyUsers(Collection<User> recipients, String message, String title,
            String type, Long proposalId, NotificationEvent event) {
        return notifyUsers(recipients, message, title, type, proposalId, null, event);
    }

    /**
     * Writes the same notification to every approved account in the given roles. The roles
     * are resolved with one query and the recipients are de-duplicated, so overlapping
     * roles can never produce two rows for one account.
     */
    public List<Notification> notifyRoles(Collection<String> roles, String message, String title,
            String type, Long proposalId) {
        return notifyRoles(roles, message, title, type, proposalId, null);
    }

    public List<Notification> notifyRoles(Collection<String> roles, String message, String title,
            String type, Long proposalId, NotificationEvent event) {
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }
        Set<String> distinct = new LinkedHashSet<>();
        for (String role : roles) {
            if (role != null && !role.isBlank()) {
                distinct.add(role.trim().toUpperCase(Locale.ROOT));
            }
        }
        if (distinct.isEmpty()) {
            return List.of();
        }
        return notifyUsers(userRepository.findByRoleIn(new ArrayList<>(distinct)),
                message, title, type, proposalId, null, event);
    }

    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        Long ownerId = saved.getUser() != null ? saved.getUser().getId() : null;
        if (ownerId != null) {
            eventPublisher.publishEvent(new NotificationChangedEvent(ownerId, "read"));
        }
        return saved;
    }

    /**
     * Marks a notification as read on behalf of the authenticated caller. The notification
     * must belong to that caller; another account's notification id is rejected with 403,
     * whatever role the caller holds.
     */
    @Transactional
    public Notification markAsReadForCurrentUser(Long notificationId, Long requesterId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        Long ownerId = notification.getUser() != null ? notification.getUser().getId() : null;
        if (ownerId == null || !ownerId.equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorized to access this notification.");
        }
        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        eventPublisher.publishEvent(new NotificationChangedEvent(requesterId, "read"));
        return saved;
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        for (Notification notification : notifications) {
            notification.setIsRead(true);
        }
        notificationRepository.saveAll(notifications);
        eventPublisher.publishEvent(new NotificationChangedEvent(userId, "read-all"));
    }
}
