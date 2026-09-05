package com.rpis.backend.service;

import com.rpis.backend.dto.ProposalAnnouncementRequest;
import com.rpis.backend.model.ProposalAnnouncement;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ProposalAnnouncementRepository;
import com.rpis.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class ProposalAnnouncementService {

    private final ProposalAnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    // ==================== READ ====================

    public List<ProposalAnnouncement> getAllAnnouncements() {
        return announcementRepository.findAll();
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

    // ==================== CREATE ====================

    @Transactional
    public ProposalAnnouncement createAnnouncement(ProposalAnnouncementRequest request) {
        validateRequest(request);

        String name = request.getName().trim();
        if (announcementRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                    "A proposal announcement named \"" + name + "\" already exists.");
        }

        validateNoOverlap(request.getStartDate(), request.getEndDate(), null);

        ProposalAnnouncement announcement = new ProposalAnnouncement();
        announcement.setName(name);
        announcement.setStartDate(request.getStartDate());
        announcement.setEndDate(request.getEndDate());
        announcement.setStatus(normalizeStatus(request.getStatus()));

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

        validateNoOverlap(request.getStartDate(), request.getEndDate(), id);

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

        // Defensively re-check the no-overlap rule in case the announcement was created
        // before this rule existed.
        validateNoOverlap(announcement.getStartDate(), announcement.getEndDate(), id);

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

        announcementRepository.delete(announcement);
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
     * Business rule: proposal announcements must not overlap. Two ranges overlap when
     * newStart <= existingEnd AND newEnd >= existingStart. Applies across all
     * statuses (Draft, Active, Closed).
     */
    private void validateNoOverlap(LocalDate startDate, LocalDate endDate, Long excludeAnnouncementId) {
        List<ProposalAnnouncement> overlapping = (excludeAnnouncementId == null)
                ? announcementRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(endDate, startDate)
                : announcementRepository.findByIdNotAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        excludeAnnouncementId, endDate, startDate);

        if (!overlapping.isEmpty()) {
            ProposalAnnouncement conflict = overlapping.get(0);
            throw new IllegalStateException(String.format(
                    "The selected date range overlaps with an existing announcement \"%s\" (%s - %s). Please choose a non-overlapping range.",
                    conflict.getName(),
                    conflict.getStartDate().format(DATE_FORMAT),
                    conflict.getEndDate().format(DATE_FORMAT)));
        }
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
