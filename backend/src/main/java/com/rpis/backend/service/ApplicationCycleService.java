package com.rpis.backend.service;

import com.rpis.backend.dto.ApplicationCycleRequest;
import com.rpis.backend.model.ApplicationCycle;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ApplicationCycleRepository;
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
public class ApplicationCycleService {

    private final ApplicationCycleRepository cycleRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy");

    // ==================== READ ====================

    public List<ApplicationCycle> getAllCycles() {
        return cycleRepository.findAll();
    }

    public ApplicationCycle getCycleById(Long id) {
        return cycleRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Application cycle not found: " + id));
    }

    /**
     * Authoritative gatekeeper check: the cycle currently accepting submissions is
     * the one with status ACTIVE whose [startDate, endDate] range contains today.
     *
     * @throws NoSuchElementException when no cycle is currently active
     */
    public ApplicationCycle getActiveCycle() {
        LocalDate today = LocalDate.now();
        return cycleRepository
                .findFirstByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        ApplicationCycle.STATUS_ACTIVE, today, today)
                .orElseThrow(() -> new NoSuchElementException(
                        "Submissions are currently closed. There is no active application cycle at this time."));
    }

    public boolean hasActiveCycle() {
        LocalDate today = LocalDate.now();
        return cycleRepository
                .findFirstByStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        ApplicationCycle.STATUS_ACTIVE, today, today)
                .isPresent();
    }

    // ==================== CREATE ====================

    @Transactional
    public ApplicationCycle createCycle(ApplicationCycleRequest request) {
        validateRequest(request);

        String name = request.getName().trim();
        if (cycleRepository.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException(
                    "An application cycle named \"" + name + "\" already exists.");
        }

        validateNoOverlap(request.getStartDate(), request.getEndDate(), null);

        ApplicationCycle cycle = new ApplicationCycle();
        cycle.setName(name);
        cycle.setStartDate(request.getStartDate());
        cycle.setEndDate(request.getEndDate());
        cycle.setStatus(normalizeStatus(request.getStatus()));

        ApplicationCycle saved = cycleRepository.save(cycle);

        // Notify all proponents when a cycle is created directly as ACTIVE.
        if (ApplicationCycle.STATUS_ACTIVE.equals(saved.getStatus())) {
            notifyAllProponents(saved);
        }

        return saved;
    }

    // ==================== UPDATE ====================

    @Transactional
    public ApplicationCycle updateCycle(Long id, ApplicationCycleRequest request) {
        ApplicationCycle cycle = getCycleById(id);

        validateRequest(request);

        String name = request.getName().trim();
        if (cycleRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException(
                    "An application cycle named \"" + name + "\" already exists.");
        }

        validateNoOverlap(request.getStartDate(), request.getEndDate(), id);

        String previousStatus = cycle.getStatus();
        String newStatus = normalizeStatus(request.getStatus());

        cycle.setName(name);
        cycle.setStartDate(request.getStartDate());
        cycle.setEndDate(request.getEndDate());
        cycle.setStatus(newStatus);

        ApplicationCycle saved = cycleRepository.save(cycle);

        // Notify all proponents only on the transition into ACTIVE so re-saving an
        // already-active cycle never sends duplicate notifications.
        if (ApplicationCycle.STATUS_ACTIVE.equals(newStatus)
                && !ApplicationCycle.STATUS_ACTIVE.equals(previousStatus)) {
            notifyAllProponents(saved);
        }

        return saved;
    }

    @Transactional
    public ApplicationCycle activateCycle(Long id) {
        ApplicationCycle cycle = getCycleById(id);

        if (ApplicationCycle.STATUS_ACTIVE.equals(cycle.getStatus())) {
            throw new IllegalArgumentException(
                    "Application cycle \"" + cycle.getName() + "\" is already active.");
        }

        if (cycle.getEndDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Cannot activate an expired cycle. The end date has already passed.");
        }

        // Defensively re-check the no-overlap rule in case the cycle was created
        // before this rule existed.
        validateNoOverlap(cycle.getStartDate(), cycle.getEndDate(), id);

        cycle.setStatus(ApplicationCycle.STATUS_ACTIVE);
        ApplicationCycle saved = cycleRepository.save(cycle);

        notifyAllProponents(saved);
        return saved;
    }

    // ==================== DELETE ====================

    @Transactional
    public void deleteCycle(Long id) {
        ApplicationCycle cycle = getCycleById(id);

        if (ApplicationCycle.STATUS_ACTIVE.equals(cycle.getStatus())) {
            throw new IllegalArgumentException(
                    "Active application cycles cannot be deleted. Close the cycle first.");
        }

        cycleRepository.delete(cycle);
    }

    // ==================== VALIDATION ====================

    private void validateRequest(ApplicationCycleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Cycle name is required.");
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
            return ApplicationCycle.STATUS_DRAFT;
        }
        String normalized = status.trim().toUpperCase();
        if (!ApplicationCycle.STATUS_DRAFT.equals(normalized)
                && !ApplicationCycle.STATUS_ACTIVE.equals(normalized)
                && !ApplicationCycle.STATUS_CLOSED.equals(normalized)) {
            throw new IllegalArgumentException(
                    "Invalid cycle status: " + status + ". Allowed values are DRAFT, ACTIVE, CLOSED.");
        }
        return normalized;
    }

    /**
     * Business rule: application cycles must not overlap. Two ranges overlap when
     * newStart <= existingEnd AND newEnd >= existingStart. Applies across all
     * statuses (Draft, Active, Closed).
     */
    private void validateNoOverlap(LocalDate startDate, LocalDate endDate, Long excludeCycleId) {
        List<ApplicationCycle> overlapping = (excludeCycleId == null)
                ? cycleRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(endDate, startDate)
                : cycleRepository.findByIdNotAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        excludeCycleId, endDate, startDate);

        if (!overlapping.isEmpty()) {
            ApplicationCycle conflict = overlapping.get(0);
            throw new IllegalStateException(String.format(
                    "The selected date range overlaps with an existing cycle \"%s\" (%s - %s). Please choose a non-overlapping range.",
                    conflict.getName(),
                    conflict.getStartDate().format(DATE_FORMAT),
                    conflict.getEndDate().format(DATE_FORMAT)));
        }
    }

    // ==================== NOTIFICATIONS ====================

    /**
     * Fan-out an in-app notification to all approved proponents announcing that a
     * new application cycle is open for submissions.
     */
    private void notifyAllProponents(ApplicationCycle cycle) {
        List<User> proponents = userRepository.findByRoleAndStatus("PROPONENT", "APPROVED");

        String title = "New Application Cycle Open";
        String message = String.format(
                "The application cycle \"%s\" is now accepting new proposal submissions from %s to %s. Please submit your proposal before the deadline.",
                cycle.getName(),
                cycle.getStartDate().format(DATE_FORMAT),
                cycle.getEndDate().format(DATE_FORMAT));

        for (User proponent : proponents) {
            notificationService.createNotification(
                    proponent.getId(), message, title, "APPLICATION_CYCLE", null);
        }
    }
}
