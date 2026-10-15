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
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProposalAnnouncementService {

    private final ProposalAnnouncementRepository announcementRepository;
    private final ProposalRepository proposalRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

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

        String previousStatus = announcement.getStatus();
        String newStatus = normalizeStatus(request.getStatus());

        announcement.setName(name);
        announcement.setStartDate(request.getStartDate());
        announcement.setEndDate(request.getEndDate());
        announcement.setStatus(newStatus);

        ProposalAnnouncement saved = announcementRepository.save(announcement);

        // Notify all proponents only on the transition into ACTIVE so re-saving an
        // already-active announcement never sends duplicate notifications.
        if (ProposalAnnouncement.STATUS_ACTIVE.equals(newStatus)
                && !ProposalAnnouncement.STATUS_ACTIVE.equals(previousStatus)) {
            notifyAllProponents(saved);
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

    // ==================== NOTIFICATIONS ====================

    /**
     * Fan-out an in-app notification to all approved proponents announcing that a
     * new proposal announcement is open for submissions.
     */
    private void notifyAllProponents(ProposalAnnouncement announcement) {
        List<User> proponents = userRepository.findByRoleAndStatus("PROPONENT", "APPROVED");

        String title = "New Proposal Announcement Open";
        String message = String.format(
                "The proposal announcement \"%s\" is now accepting new proposal submissions from %s to %s. Please submit your proposal before the deadline.",
                announcement.getName(),
                announcement.getStartDate().format(DATE_FORMAT),
                announcement.getEndDate().format(DATE_FORMAT));

        for (User proponent : proponents) {
            notificationService.createNotification(
                    proponent.getId(), message, title, "PROPOSAL_ANNOUNCEMENT", null);
        }
    }
}
