package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalAnnouncementRequest;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import com.rpis.backend.repository.ProposalRepository;
import com.rpis.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProposalAnnouncementService {

    private final ProposalAnnouncementRepository announcementRepository;
    private final ProposalRepository proposalRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    /**
     * Statuses proponents actually see (mirrors {@code src/utils/announcementStatus.js}):
     * a stored ACTIVE announcement is <em>Scheduled</em> until its start date arrives,
     * <em>Open</em> while today is inside its window, and <em>Closed</em> once the window
     * has passed. Only OPEN and CLOSED are eligible for the "announcement updated"
     * notification.
     */
    private static final String DISPLAY_DRAFT = "DRAFT";
    private static final String DISPLAY_SCHEDULED = "SCHEDULED";
    private static final String DISPLAY_OPEN = "OPEN";
    private static final String DISPLAY_CLOSED = "CLOSED";

    // ==================== READ ====================

    public List<ProposalAnnouncement> getAllAnnouncements() {
        // Newest first: Start Date descending, then creation order (most recently
        // created first) so announcements sharing a start date stay predictable.
        List<ProposalAnnouncement> announcements =
                announcementRepository.findAllByOrderByStartDateDescCreatedAtDescIdDesc();

        Set<Long> announcementIdsWithSubmissions = new HashSet<>(
                proposalRepository.findAnnouncementIdsWithSubmittedProposals());

        for (ProposalAnnouncement announcement : announcements) {
            announcement.setHasSubmittedProposals(
                    announcementIdsWithSubmissions.contains(announcement.getId()));
        }

        return announcements;
    }

    public ProposalAnnouncement getAnnouncementById(Long id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Proposal announcement not found: " + id));
    }

    /**
     * Read-only Call for Proposals details for the Proponent view that a notification
     * opens. Draft announcements are still being prepared by RPS/OVCRIGE and are never
     * visible to proponents anywhere else in RPIS, so they are reported as not found
     * rather than exposed.
     *
     * @throws NoSuchElementException when the announcement does not exist or is a draft
     */
    public ProposalAnnouncement getAnnouncementForProponents(Long id) {
        ProposalAnnouncement announcement = getAnnouncementById(id);
        if (ProposalAnnouncement.STATUS_DRAFT.equals(announcement.getStatus())) {
            throw new NoSuchElementException("Proposal announcement not found: " + id);
        }
        return announcement;
    }

    /**
     * Authoritative gatekeeper check: the announcement currently accepting submissions is
     * the one with status ACTIVE whose [startDate, endDate] range contains today.
     *
     * @throws NoSuchElementException when no announcement is currently active
     */
    public ProposalAnnouncement getActiveAnnouncement() {
        LocalDate today = LocalDate.now();
        return announcementRepository
                .findFirstByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        ProposalAnnouncement.STATUS_ACTIVE, today, today)
                .orElseThrow(() -> new NoSuchElementException(
                        "Submissions are currently closed. There is no active proposal announcement at this time."));
    }

    public boolean hasActiveAnnouncement() {
        LocalDate today = LocalDate.now();
        return announcementRepository
                .findFirstByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        ProposalAnnouncement.STATUS_ACTIVE, today, today)
                .isPresent();
    }

    /**
     * Returns the announcement currently accepting submissions, or {@code null} when
     * there is no active announcement. Used by the proposal save flow so the proposal
     * can be associated with the active submission window.
     */
    public ProposalAnnouncement getActiveAnnouncementOrNull() {
        LocalDate today = LocalDate.now();
        return announcementRepository
                .findFirstByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        ProposalAnnouncement.STATUS_ACTIVE, today, today)
                .orElse(null);
    }

    /**
     * Every Call for Proposal a proponent can currently respond to: status ACTIVE and
     * today inside [startDate, endDate], newest window first. This is the same rule the
     * submission gatekeeper applies ({@link #getActiveAnnouncementOrNull()}), returned
     * as a list because more than one call may be open at the same time.
     * <p>
     * Drafts, scheduled (start date still ahead), closed and past-deadline calls are
     * never part of the result, so the proponent can only pick a genuinely open call.
     */
    public List<ProposalAnnouncement> getOpenAnnouncements() {
        LocalDate today = LocalDate.now();
        return announcementRepository
                .findByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDescIdDesc(
                        ProposalAnnouncement.STATUS_ACTIVE, today, today);
    }

    /**
     * The Calls for Proposal the first step of the Create Proposal workflow shows: the
     * ones that are open for submissions right now and the published ones that open
     * later (scheduled). Both are real saved records - the page never invents a call.
     * <p>
     * Only status ACTIVE counts (an administrator's DRAFT or CLOSED call stays hidden
     * even when its dates would allow submissions), and a call whose deadline has
     * passed is never returned. Order: the open calls first (newest submission window
     * first, exactly as {@link #getOpenAnnouncements()} returns them), then the
     * upcoming calls by their opening date, earliest first - so the call a proponent
     * can apply to right now is always at the top.
     */
    public List<ProposalAnnouncement> getAvailableAnnouncements() {
        LocalDate today = LocalDate.now();
        List<ProposalAnnouncement> published = announcementRepository
                .findByStatusAndEndDateGreaterThanEqualOrderByStartDateAscIdAsc(
                        ProposalAnnouncement.STATUS_ACTIVE, today);

        List<ProposalAnnouncement> open = new ArrayList<>();
        List<ProposalAnnouncement> scheduled = new ArrayList<>();
        for (ProposalAnnouncement announcement : published) {
            // "Open" is exactly the rule the submission gatekeeper uses; anything that
            // has not started yet is upcoming.
            if (DISPLAY_OPEN.equals(displayStatus(announcement, today))) {
                open.add(announcement);
            } else if (DISPLAY_SCHEDULED.equals(displayStatus(announcement, today))) {
                scheduled.add(announcement);
            }
        }

        open.sort(Comparator.comparing(ProposalAnnouncement::getStartDate)
                .reversed()
                .thenComparing(ProposalAnnouncement::getId, Comparator.reverseOrder()));
        scheduled.sort(Comparator.comparing(ProposalAnnouncement::getStartDate)
                .thenComparing(ProposalAnnouncement::getId));

        List<ProposalAnnouncement> available = new ArrayList<>(open);
        available.addAll(scheduled);
        return available;
    }

    /**
     * The Call for Proposal the proponent selected in the Create Proposal workflow. It
     * must still be open for submissions - the exact rule the UI shows and the
     * submission gatekeeper enforces - so a proposal can never be created or submitted
     * against a call that closed (or has not opened) in the meantime.
     *
     * @throws IllegalArgumentException when the call does not exist
     * @throws IllegalStateException    when the call is not open for submissions
     */
    public ProposalAnnouncement requireOpenAnnouncement(Long id) {
        ProposalAnnouncement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Call for Proposal not found: " + id));
        if (!DISPLAY_OPEN.equals(displayStatus(announcement, LocalDate.now()))) {
            throw new IllegalStateException(
                    "The selected Call for Proposal is no longer open for submissions. "
                            + "Please select an open call for proposal.");
        }
        return announcement;
    }

    // ==================== CREATE ====================

    @Transactional
    public ProposalAnnouncement createAnnouncement(ProposalAnnouncementRequest request) {
        validateRequest(request);

        String name = request.getName().trim();
        if (announcementRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                    "A proposal announcement named \"" + name + "\" already exists.");
        }

        ProposalAnnouncement announcement = new ProposalAnnouncement();
        announcement.setName(name);
        announcement.setStartDate(request.getStartDate());
        announcement.setEndDate(request.getEndDate());
        // Newly created announcements default to ACTIVE so RPS Staff do not need to
        // manually select Active when opening a new submission window.
        String status = (request.getStatus() == null || request.getStatus().trim().isEmpty())
                ? ProposalAnnouncement.STATUS_ACTIVE
                : normalizeStatus(request.getStatus());
        announcement.setStatus(status);

        // RIIDE priority agendas are optional: an announcement may be created with
        // none selected, and only known program keys are persisted.
        announcement.setPriorityAgendas(
                ProposalAnnouncementCatalog.normalizeProgramKeys(request.getPriorityAgendas()));
        // A brand-new announcement starts from the standard reminder set; when the
        // client sends an explicit selection (including an empty one) that selection
        // is stored exactly as sent.
        announcement.setReminderIds(ProposalAnnouncementCatalog.normalizeReminderIds(
                request.getReminderIds() == null
                        ? ProposalAnnouncementCatalog.defaultReminderIds()
                        : request.getReminderIds()));
        announcement.setAdditionalAnnouncement(
                normalizeAdditionalAnnouncement(request.getAdditionalAnnouncement()));

        ProposalAnnouncement saved = announcementRepository.save(announcement);

        // Notify all proponents when an announcement is created directly as ACTIVE.
        if (ProposalAnnouncement.STATUS_ACTIVE.equals(saved.getStatus())) {
            notifyAllProponents(saved);
        }

        return saved;
    }

    // ==================== UPDATE ====================

    @Transactional
    public ProposalAnnouncement updateAnnouncement(Long id, ProposalAnnouncementRequest request) {
        ProposalAnnouncement announcement = getAnnouncementById(id);

        validateRequest(request);

        String name = request.getName().trim();
        if (announcementRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                    "A proposal announcement named \"" + name + "\" already exists.");
        }

        // Snapshot the persisted state before the edit. Eligibility for the
        // "announcement updated" notification depends on the status proponents saw
        // BEFORE the change (a Scheduled call is never eligible, even when the saved
        // status becomes ACTIVE or CLOSED), and on whether anything actually changed.
        LocalDate today = LocalDate.now();
        String previousStatus = announcement.getStatus();
        String previousDisplayStatus = displayStatus(announcement, today);
        String previousName = announcement.getName();
        LocalDate previousStartDate = announcement.getStartDate();
        LocalDate previousEndDate = announcement.getEndDate();
        List<String> previousPriorityAgendas = copyOf(announcement.getPriorityAgendas());
        List<String> previousReminderIds = copyOf(announcement.getReminderIds());
        String previousAdditionalAnnouncement = announcement.getAdditionalAnnouncement();

        String newStatus = normalizeStatus(request.getStatus());

        announcement.setName(name);
        announcement.setStartDate(request.getStartDate());
        announcement.setEndDate(request.getEndDate());
        announcement.setStatus(newStatus);

        // Only replace the new fields when the client actually sent them, so an update
        // that does not touch them never resets saved selections back to the
        // new-announcement defaults and never wipes the optional text.
        if (request.getPriorityAgendas() != null) {
            announcement.setPriorityAgendas(
                    ProposalAnnouncementCatalog.normalizeProgramKeys(request.getPriorityAgendas()));
        }
        if (request.getReminderIds() != null) {
            announcement.setReminderIds(
                    ProposalAnnouncementCatalog.normalizeReminderIds(request.getReminderIds()));
        }
        if (request.getAdditionalAnnouncement() != null) {
            announcement.setAdditionalAnnouncement(
                    normalizeAdditionalAnnouncement(request.getAdditionalAnnouncement()));
        }

        ProposalAnnouncement saved = announcementRepository.save(announcement);

        // Notify all proponents only on the transition into ACTIVE so re-saving an
        // already-active announcement never sends duplicate notifications.
        if (ProposalAnnouncement.STATUS_ACTIVE.equals(newStatus)
                && !ProposalAnnouncement.STATUS_ACTIVE.equals(previousStatus)) {
            notifyAllProponents(saved);
        }

        // Existing Open / Closed calls: one notification per eligible proponent when the
        // edit actually changed something (status, deadline, name or another editable
        // detail). This runs after the save inside the same transaction, so a rejected or
        // rolled-back update never notifies anyone, and a Scheduled announcement is
        // excluded because eligibility uses the pre-edit display status.
        if (isOpenOrClosed(previousDisplayStatus)) {
            boolean deadlineChanged = !Objects.equals(previousEndDate, saved.getEndDate());
            boolean contentChanged = !Objects.equals(previousName, saved.getName())
                    || !Objects.equals(previousStartDate, saved.getStartDate())
                    || !Objects.equals(previousStatus, saved.getStatus())
                    || !Objects.equals(previousPriorityAgendas, copyOf(saved.getPriorityAgendas()))
                    || !Objects.equals(previousReminderIds, copyOf(saved.getReminderIds()))
                    || !Objects.equals(previousAdditionalAnnouncement, saved.getAdditionalAnnouncement());
            if (deadlineChanged || contentChanged) {
                notifyProponentsOfUpdate(
                        saved, previousDisplayStatus, displayStatus(saved, today), deadlineChanged);
            }
        }

        return saved;
    }

    @Transactional
    public ProposalAnnouncement activateAnnouncement(Long id) {
        ProposalAnnouncement announcement = getAnnouncementById(id);

        if (ProposalAnnouncement.STATUS_ACTIVE.equals(announcement.getStatus())) {
            throw new IllegalArgumentException(
                    "Proposal announcement \"" + announcement.getName() + "\" is already active.");
        }

        if (announcement.getEndDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Cannot activate an expired announcement. The end date has already passed.");
        }

        announcement.setStatus(ProposalAnnouncement.STATUS_ACTIVE);
        ProposalAnnouncement saved = announcementRepository.save(announcement);

        notifyAllProponents(saved);
        return saved;
    }

    // ==================== DELETE ====================

    @Transactional
    public void deleteAnnouncement(Long id) {
        ProposalAnnouncement announcement = getAnnouncementById(id);

        if (ProposalAnnouncement.STATUS_ACTIVE.equals(announcement.getStatus())) {
            throw new IllegalArgumentException(
                    "Active proposal announcements cannot be deleted. Close the announcement first.");
        }

        if (hasSubmittedProposals(id)) {
            throw new IllegalArgumentException(
                    "This proposal announcement cannot be deleted because one or more proponents have already submitted proposals under it.");
        }

        announcementRepository.delete(announcement);
    }

    private boolean hasSubmittedProposals(Long announcementId) {
        return proposalRepository.findAnnouncementIdsWithSubmittedProposals().contains(announcementId);
    }

    // ==================== VALIDATION ====================

    private void validateRequest(ProposalAnnouncementRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Announcement name is required.");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required.");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date cannot be earlier than the start date.");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return ProposalAnnouncement.STATUS_DRAFT;
        }
        String normalized = status.trim().toUpperCase();
        if (!ProposalAnnouncement.STATUS_DRAFT.equals(normalized)
                && !ProposalAnnouncement.STATUS_ACTIVE.equals(normalized)
                && !ProposalAnnouncement.STATUS_CLOSED.equals(normalized)) {
            throw new IllegalArgumentException(
                    "Invalid announcement status: " + status + ". Allowed values are DRAFT, ACTIVE, CLOSED.");
        }
        return normalized;
    }

    /**
     * Optional announcement text: leading/trailing whitespace is trimmed (inner
     * line breaks are preserved) and blank input is stored as {@code null} instead
     * of an empty string.
     */
    private String normalizeAdditionalAnnouncement(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    // ==================== STATUS HELPERS ====================

    /**
     * The status proponents actually see for an announcement: DRAFT, SCHEDULED, OPEN or
     * CLOSED. Mirrors {@code src/utils/announcementStatus.js} so the notification rules
     * agree with the status shown in the UI.
     */
    private static String displayStatus(ProposalAnnouncement announcement, LocalDate today) {
        if (ProposalAnnouncement.STATUS_DRAFT.equals(announcement.getStatus())) {
            return DISPLAY_DRAFT;
        }
        if (ProposalAnnouncement.STATUS_CLOSED.equals(announcement.getStatus())) {
            return DISPLAY_CLOSED;
        }
        if (announcement.getStartDate() != null && announcement.getStartDate().isAfter(today)) {
            return DISPLAY_SCHEDULED;
        }
        if (announcement.getEndDate() != null && announcement.getEndDate().isBefore(today)) {
            return DISPLAY_CLOSED;
        }
        return DISPLAY_OPEN;
    }

    /** True only for the statuses that make an announcement eligible for an update notification. */
    private static boolean isOpenOrClosed(String displayStatus) {
        return DISPLAY_OPEN.equals(displayStatus) || DISPLAY_CLOSED.equals(displayStatus);
    }

    /** Null-safe copy used to compare the editable list fields before and after an edit. */
    private static List<String> copyOf(List<String> values) {
        return values == null ? List.of() : new ArrayList<>(values);
    }

    // ==================== NOTIFICATIONS ====================

    /**
     * Fan-out an in-app notification to all approved proponents announcing that a
     * new proposal announcement is open for submissions.
     */
    private void notifyAllProponents(ProposalAnnouncement announcement) {
        String title = "New Proposal Announcement Open";
        String message = String.format(
                "The proposal announcement \"%s\" is now accepting new proposal submissions from %s to %s. Please submit your proposal before the deadline.",
                announcement.getName(),
                announcement.getStartDate().format(DATE_FORMAT),
                announcement.getEndDate().format(DATE_FORMAT));

        fanOutToProponents(announcement, title, message);
    }

    /**
     * Fan-out an in-app notification to all approved proponents telling them that an
     * existing Call for Proposals was updated. One notification per proponent, using the
     * most specific message that applies to the detected change.
     */
    private void notifyProponentsOfUpdate(ProposalAnnouncement announcement,
            String previousDisplayStatus, String newDisplayStatus, boolean deadlineChanged) {
        String title;
        String message;
        if (DISPLAY_OPEN.equals(previousDisplayStatus) && DISPLAY_CLOSED.equals(newDisplayStatus)) {
            title = "Call for Proposals Closed";
            message = String.format(
                    "The Call for Proposals \"%s\" has been closed. Please review the updated announcement details.",
                    announcement.getName());
        } else if (deadlineChanged) {
            title = "Submission Deadline Updated";
            message = String.format(
                    "The submission deadline for \"%s\" has been updated. Please review the latest deadline.",
                    announcement.getName());
        } else {
            title = "Call for Proposals Updated";
            message = String.format(
                    "The Call for Proposals \"%s\" has been updated. Please review the latest announcement details and submission deadline.",
                    announcement.getName());
        }

        fanOutToProponents(announcement, title, message);
    }

    /**
     * The established recipient group for Call for Proposals notifications: every
     * approved Proponent account, exactly one notification each. Each notification
     * carries the announcement it was raised for so the Proponent notification list can
     * open that exact Call for Proposals.
     */
    private void fanOutToProponents(ProposalAnnouncement announcement, String title, String message) {
        List<User> proponents = userRepository.findByRoleAndStatus("PROPONENT", "APPROVED");

        for (User proponent : proponents) {
            // proposalId stays null, exactly like the existing "new announcement"
            // notification: the field references a proposal, while the announcement
            // itself is carried by proposalAnnouncementId.
            notificationService.createNotification(
                    proponent.getId(), message, title, "PROPOSAL_ANNOUNCEMENT", null,
                    announcement.getId());
        }
    }
}
