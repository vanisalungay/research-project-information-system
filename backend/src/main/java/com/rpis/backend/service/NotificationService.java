package com.rpis.backend.service;

import com.rpis.backend.model.Notification;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.NotificationRepository;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.util.AnnouncementStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
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

    public List<Notification> getNotificationsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        List<Notification> notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        fillAnnouncementDetails(notifications);
        return notifications;
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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setTitle(title != null ? title : "Notification");
        notification.setType(type);
        notification.setProposalId(proposalId);
        notification.setProposalAnnouncementId(proposalAnnouncementId);
        notification.setIsRead(false);
        return notificationRepository.save(notification);
    }

    @Transactional
    public Notification markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }

    /**
     * Marks a notification as read on behalf of the authenticated caller. When the
     * caller is restricted to their own records (a Proponent), the notification must
     * belong to them; otherwise the request is rejected with 403.
     */
    @Transactional
    public Notification markAsReadForCurrentUser(Long notificationId, Long requesterId, boolean restrictToOwner) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        if (restrictToOwner) {
            Long ownerId = notification.getUser() != null ? notification.getUser().getId() : null;
            if (ownerId == null || !ownerId.equals(requesterId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You are not authorized to access this notification.");
            }
        }
        notification.setIsRead(true);
        return notificationRepository.save(notification);
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
    }
}
