package com.rpis.backend.service;

import com.rpis.backend.model.EndorsementRequest;
import com.rpis.backend.model.Notification;
import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.EndorsementRequestRepository;
import com.rpis.backend.repository.NotificationRepository;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.util.AnnouncementStatus;
import com.rpis.backend.util.EmailNormalizer;
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
import java.util.Comparator;
import java.util.HashMap;
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
    private final EndorsementRequestRepository endorsementRequestRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationPreferencePolicy preferencePolicy;

    /** Every notification of the account, regardless of role (administrative/legacy use). */
    public List<Notification> getNotificationsForUser(Long userId) {
        return getNotificationsForUser(userId, null);
    }

    /**
     * The notifications of the account <em>in one role</em>. A multi-role account sees
     * only the notifications raised for the role it is currently signed in as, so a
     * Proponent notification never appears while it is using its Dean dashboard.
     *
     * <p>A legacy row with no role is served only to an account that holds a single role:
     * for such an account there is no other role the row could belong to. A multi-role
     * account is never shown an unscoped row, because it could not be attributed safely.
     */
    public List<Notification> getNotificationsForUser(Long userId, String recipientRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        String role = EmailNormalizer.normalizeRole(recipientRole);
        List<Notification> notifications;
        if (role == null) {
            notifications = new ArrayList<>(
                    notificationRepository.findByUserOrderByCreatedAtDesc(user));
        } else {
            notifications = new ArrayList<>(
                    notificationRepository
                            .findByUserAndRecipientRoleIgnoreCaseOrderByCreatedAtDesc(user, role));
            if (user.getRoles().size() <= 1) {
                notifications.addAll(notificationRepository
                        .findByUserAndRecipientRoleIsNullOrderByCreatedAtDesc(user));
                notifications.sort(Comparator.comparing(
                        Notification::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())));
            }
        }
        fillAnnouncementDetails(notifications);
        fillProposalDocumentIds(notifications);
        fillLinkedProposalIds(notifications);
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
     * Fills the computed (not persisted) proposal link the client should open.
     *
     * <p>{@code proposalId} alone is not safe to navigate by: it is an endorsement request
     * id on an {@code ENDORSEMENT} row, and historical rows of that type were also written
     * with a proposal id, so the value is polymorphic. A deleted proposal is also still a
     * non-null id. This method therefore verifies the record against the database:
     * <ul>
     *   <li>proposal-workflow rows link to the proposal only when it still exists;</li>
     *   <li>{@code ENDORSEMENT} rows link to the proposal directly when the id is a
     *       proposal (legacy forwarding rows), otherwise through the endorsement request;
     *   <li>anything that resolves to nothing stays null, and the client shows its
     *       "nothing to open" message instead of navigating to a broken record.</li>
     * </ul>
     */
    private void fillLinkedProposalIds(List<Notification> notifications) {
        Set<Long> workflowIds = new HashSet<>();
        Set<Long> endorsementIds = new HashSet<>();
        for (Notification notification : notifications) {
            if (notification.getProposalId() == null) {
                continue;
            }
            if (NotificationTypes.isProposalWorkflow(notification.getType())) {
                workflowIds.add(notification.getProposalId());
            } else if (NotificationTypes.ENDORSEMENT.equalsIgnoreCase(notification.getType())) {
                endorsementIds.add(notification.getProposalId());
            }
        }

        Set<Long> existingWorkflowIds = existingProposalIds(workflowIds);

        // An ENDORSEMENT id may itself be a proposal (the historical forwarding rows) or an
        // endorsement request whose linked proposal we must look up.
        Set<Long> endorsementIdsThatAreProposals = existingProposalIds(endorsementIds);
        Set<Long> requestIds = new HashSet<>(endorsementIds);
        requestIds.removeAll(endorsementIdsThatAreProposals);

        Map<Long, Long> proposalByRequestId = new HashMap<>();
        if (!requestIds.isEmpty()) {
            for (EndorsementRequest request : endorsementRequestRepository.findAllById(requestIds)) {
                if (request != null && request.getId() != null && request.getProposalId() != null) {
                    proposalByRequestId.put(request.getId(), request.getProposalId());
                }
            }
        }
        Set<Long> linkedByRequest = existingProposalIds(new HashSet<>(proposalByRequestId.values()));

        for (Notification notification : notifications) {
            Long proposalId = notification.getProposalId();
            if (proposalId == null) {
                notification.setLinkedProposalId(null);
                continue;
            }
            Long linked = null;
            if (NotificationTypes.isProposalWorkflow(notification.getType())) {
                linked = existingWorkflowIds.contains(proposalId) ? proposalId : null;
            } else if (NotificationTypes.ENDORSEMENT.equalsIgnoreCase(notification.getType())) {
                if (endorsementIdsThatAreProposals.contains(proposalId)) {
                    linked = proposalId;
                } else {
                    Long requestProposalId = proposalByRequestId.get(proposalId);
                    linked = requestProposalId != null && linkedByRequest.contains(requestProposalId)
                            ? requestProposalId
                            : null;
                }
            }
            notification.setLinkedProposalId(linked);
        }
    }

    /** The ids that still exist in {@code proposals}, with one query for the whole batch. */
    private Set<Long> existingProposalIds(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return proposalRepository.findAllById(ids).stream()
                .map(Proposal::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
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
        return createNotification(userId, message, title, type, proposalId,
                proposalAnnouncementId, event, null);
    }

    /**
     * Creates a notification in an explicit recipient role. Multi-role accounts must pass
     * the role context here, or the row is left unscoped and is only visible to a
     * single-role account.
     */
    public Notification createNotification(Long userId, String message, String title, String type,
            Long proposalId, Long proposalAnnouncementId, NotificationEvent event,
            String recipientRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        return createNotificationForUser(user, message, title, type, proposalId,
                proposalAnnouncementId, event, recipientRole);
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
        return createNotificationForUser(user, message, title, type, proposalId,
                proposalAnnouncementId, event, null);
    }

    public Notification createNotificationForUser(User user, String message, String title,
            String type, Long proposalId, Long proposalAnnouncementId, NotificationEvent event,
            String recipientRole) {
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
        notification.setRecipientRole(recipientRoleFor(user, recipientRole));
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
     * The role the row belongs to. An explicit role is accepted only when the recipient
     * actually holds it; otherwise a single-role account's one role is used. For a
     * multi-role account with no explicit context the row stays unscoped rather than
     * being guessed, and is not shown in any role-specific list.
     */
    private static String recipientRoleFor(User user, String requested) {
        String role = EmailNormalizer.normalizeRole(requested);
        if (role != null && user.hasRole(role)) {
            return role;
        }
        List<String> active = user.getRoles();
        return active.size() == 1 ? active.get(0) : null;
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
        return notifyUsers(recipients, message, title, type, proposalId,
                proposalAnnouncementId, event, null);
    }

    /**
     * Notifies a known set of accounts in the given role context (for example the
     * Proponent receiving an approval). The role is applied where the account holds it;
     * otherwise the single-role fallback of {@link #recipientRoleFor} applies.
     */
    public List<Notification> notifyUsers(Collection<User> recipients, String message, String title,
            String type, Long proposalId, Long proposalAnnouncementId, NotificationEvent event,
            String recipientRole) {
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
                    user, message, title, type, proposalId, proposalAnnouncementId, event,
                    recipientRole);
            if (notification != null) {
                created.add(notification);
            }
        }
        return created;
    }

    public List<Notification> notifyUsers(Collection<User> recipients, String message, String title,
            String type, Long proposalId) {
        return notifyUsers(recipients, message, title, type, proposalId,
                (Long) null, (NotificationEvent) null, null);
    }

    public List<Notification> notifyUsers(Collection<User> recipients, String message, String title,
            String type, Long proposalId, NotificationEvent event, String recipientRole) {
        return notifyUsers(recipients, message, title, type, proposalId, null, event,
                recipientRole);
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
        // The assignment table is included, so a multi-role account receives the alert
        // once, tagged with the first role it holds from this request. It appears only in
        // that role's notification view, never mixed into the other role's.
        Map<Long, User> recipients = new LinkedHashMap<>();
        for (User user : userRepository.findByAnyAssignedRoleIn(new ArrayList<>(distinct))) {
            if (user != null && user.getId() != null) {
                recipients.putIfAbsent(user.getId(), user);
            }
        }
        List<Notification> created = new ArrayList<>(recipients.size());
        for (User user : recipients.values()) {
            String role = distinct.stream()
                    .filter(user::hasRole)
                    .findFirst()
                    .orElse(null);
            Notification notification = createNotificationForUser(
                    user, message, title, type, proposalId, null, event, role);
            if (notification != null) {
                created.add(notification);
            }
        }
        return created;
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
        return markAsReadForCurrentUser(notificationId, requesterId, null);
    }

    /**
     * Marks one notification read in the caller's current role. A notification raised for
     * another role of the same account is refused, so switching roles can never mark the
     * other role's notifications read. Legacy unscoped rows are accepted only when the
     * account has a single role.
     */
    @Transactional
    public Notification markAsReadForCurrentUser(Long notificationId, Long requesterId,
            String requesterRole) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + notificationId));
        User owner = notification.getUser();
        Long ownerId = owner != null ? owner.getId() : null;
        if (ownerId == null || !ownerId.equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorized to access this notification.");
        }
        String role = EmailNormalizer.normalizeRole(requesterRole);
        if (role != null) {
            String assigned = EmailNormalizer.normalizeRole(notification.getRecipientRole());
            boolean allowed = assigned != null
                    ? assigned.equals(role)
                    : owner.getRoles().size() <= 1 && owner.hasRole(role);
            if (!allowed) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "This notification belongs to a different role of this account.");
            }
        }
        notification.setIsRead(true);
        Notification saved = notificationRepository.save(notification);
        eventPublisher.publishEvent(new NotificationChangedEvent(requesterId, "read"));
        return saved;
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        markAllAsRead(userId, null);
    }

    /** Marks read only the notifications visible in the caller's current role. */
    @Transactional
    public void markAllAsRead(Long userId, String requesterRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        String role = EmailNormalizer.normalizeRole(requesterRole);
        List<Notification> notifications;
        if (role == null) {
            notifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        } else {
            notifications = new ArrayList<>(notificationRepository
                    .findByUserAndRecipientRoleIgnoreCaseOrderByCreatedAtDesc(user, role));
            if (user.getRoles().size() <= 1) {
                notifications.addAll(notificationRepository
                        .findByUserAndRecipientRoleIsNullOrderByCreatedAtDesc(user));
            }
        }
        for (Notification notification : notifications) {
            notification.setIsRead(true);
        }
        notificationRepository.saveAll(notifications);
        eventPublisher.publishEvent(new NotificationChangedEvent(userId, "read-all"));
    }
}
