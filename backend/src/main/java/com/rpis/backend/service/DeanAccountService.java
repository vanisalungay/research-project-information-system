package com.rpis.backend.service;

import com.rpis.backend.dto.DeanAccountRequest;
import com.rpis.backend.dto.DeanAssignmentResponse;
import com.rpis.backend.dto.DeanReplacementRequest;
import com.rpis.backend.model.ActivityLog;
import com.rpis.backend.model.DeanAssignment;
import com.rpis.backend.model.EndorsementRequestEvent;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.DeanAssignmentRepository;
import com.rpis.backend.repository.EndorsementRequestEventRepository;
import com.rpis.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Administrative management of the Dean / Department Chair role.
 *
 * <p>The Dean is an ordinary RPIS account with the Dean role - never a second
 * account system - so this service either creates one account or assigns the role to
 * an account that already exists (which is the way a sitting Dean who is already
 * registered as faculty is promoted, without a duplicate account).
 *
 * <p>Only an RPS_ADMIN reaches these operations (enforced by SecurityConfig and
 * re-checked here), and only the Dean role can be granted through them, so the
 * endpoint can never be used to hand out staff or administrator privileges. Every
 * assignment, revocation and scope change is written to the audit trail with the
 * responsible administrator.
 */
@Service
@RequiredArgsConstructor
public class DeanAccountService {

    /**
     * The Dean / Department Chair role identifier. The value comes from
     * {@link UserRoles#DEAN}: the Dean is an independent role, not an RPS sub-role.
     */
    public static final String ROLE_DEAN = UserRoles.DEAN;

    /** The role an account is returned to when the Dean role is revoked. */
    public static final String ROLE_PROPONENT = UserRoles.PROPONENT;

    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final Set<String> ALLOWED_STATUSES = Set.of(STATUS_APPROVED, STATUS_INACTIVE);

    private static final String INSTITUTIONAL_DOMAIN = InstitutionalCredentialsService.INSTITUTIONAL_DOMAIN;

    private final UserRepository userRepository;
    private final EndorsementRequestEventRepository eventRepository;
    private final DeanAssignmentRepository deanAssignmentRepository;
    private final PasswordService passwordService;
    private final ActivityLogService activityLogService;

    /** Every Dean account, by name. */
    public List<User> listDeans() {
        return userRepository.findByRoleOrderByNameAsc(ROLE_DEAN);
    }

    // ===== Appointment history (Dean Role Management) =====

    /**
     * The Dean appointment history the administrator's Dean Role Management table shows.
     *
     * @param filter {@code ACTIVE} (default), {@code REVOKED} or {@code ALL}
     */
    public List<DeanAssignmentResponse> listAssignments(String filter) {
        String normalized = filter == null ? DeanAssignment.STATUS_ACTIVE
                : filter.trim().toUpperCase(Locale.ROOT);
        List<DeanAssignment> rows;
        if (normalized.isEmpty() || "ALL".equals(normalized)) {
            rows = deanAssignmentRepository.findAllByOrderByDateAssignedDesc();
        } else if (DeanAssignment.STATUS_REVOKED.equals(normalized)) {
            rows = deanAssignmentRepository.findByStatusOrderByDateAssignedDesc(DeanAssignment.STATUS_REVOKED);
        } else {
            rows = deanAssignmentRepository.findByStatusOrderByDateAssignedDesc(DeanAssignment.STATUS_ACTIVE);
        }
        return rows.stream().map(DeanAssignmentResponse::of).toList();
    }

    /**
     * The accounts that may be appointed Dean of a college: the sitting Deans who are free to
     * take up another appointment. Only an approved account whose system role is
     * {@link UserRoles#DEAN} qualifies - every office role (RPS_ADMIN, RPS_STAFF, OVCRIGE,
     * OVCAF, REC, OC) and every non-Dean account is excluded - and the account must hold no
     * active appointment, which excludes the outgoing Dean of the unit being managed and any
     * Dean already assigned to another college.
     *
     * <p>The unit is the appointment's college; it selects the outgoing Dean to exclude.
     */
    public List<User> listCandidates(String college) {
        Set<Long> currentDeans = new HashSet<>();
        for (DeanAssignment assignment : activeAssignmentsForCollege(blankToNull(college))) {
            currentDeans.add(assignment.getUserId());
        }
        List<User> candidates = new ArrayList<>();
        for (User user : userRepository.findAll()) {
            if (currentDeans.contains(user.getId())) {
                continue; // the outgoing Dean is never a candidate
            }
            if (isEligibleReplacement(user)) {
                // The password is never serialized (@JsonProperty WRITE_ONLY), so it is left
                // as-is: clearing it on a managed entity would only dirty the row.
                candidates.add(user);
            }
        }
        candidates.sort(Comparator.comparing(User::getName, String.CASE_INSENSITIVE_ORDER));
        return candidates;
    }

    /**
     * The eligibility rule for a replacement Dean, in one place so the candidate list and the
     * replacement itself can never disagree: an approved account that holds the {@code DEAN}
     * role and no active appointment. A direct API call is held to exactly the same rule.
     */
    private boolean isEligibleReplacement(User user) {
        return STATUS_APPROVED.equalsIgnoreCase(user.getStatus())
                && ROLE_DEAN.equalsIgnoreCase(user.getRole())
                && !hasActiveAssignment(user.getId());
    }

    /**
     * Replaces the sitting Dean of a college (or the institution-wide fallback) with a new
     * appointment, preserving the previous Dean as history.
     *
     * <p>All of it runs in one transaction, so the college can never end up with two active
     * Deans or with none because of a partial failure:
     * <ol>
     *   <li>every ACTIVE appointment of the college (and any other ACTIVE appointment the
     *       incoming Dean already holds) is closed as {@code REVOKED}, stamped with the
     *       date and the acting administrator;</li>
     *   <li>an outgoing Dean who no longer holds any active appointment is returned to the
     *       Faculty Proponent role - their account, its records and the closed appointment
     *       are all kept;</li>
     *   <li>the incoming Dean's account is given the Dean role and the college scope, and a
     *       new ACTIVE appointment is opened for it.</li>
     * </ol>
     */
    @Transactional
    public DeanAssignmentResponse replaceDean(DeanReplacementRequest request, Long actorId,
            String actorRole) {
        requireAdministrator(actorRole);
        User actor = requireAccount(actorId);

        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("Select the account that will be the new Dean.");
        }
        User replacement = requireAccount(request.getUserId());
        String college = blankToNull(request.getAssignedCollege());
        String designation = blankToNull(request.getDesignation());

        // The outgoing Dean of this unit is never the replacement.
        Long currentDeanId = activeAssignmentsForCollege(college).stream()
                .map(DeanAssignment::getUserId)
                .findFirst()
                .orElse(null);
        if (currentDeanId != null && currentDeanId.equals(replacement.getId())) {
            throw new IllegalArgumentException(
                    "The replacement must be a different person from the current Dean.");
        }
        // The authoritative check: only an active account holding the DEAN role, with no
        // appointment elsewhere, may be appointed. The candidate list already filters, but
        // this is what a direct API call is held to, and nothing is written before it passes.
        if (!isEligibleReplacement(replacement)) {
            throw new IllegalArgumentException(
                    "The selected account is not an eligible Dean. Choose an active account with the Dean role that does not already hold another assignment.");
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Close the outgoing appointment(s): the sitting Dean of this college, plus any
        //    other active appointment the incoming Dean already holds (nobody holds two).
        List<DeanAssignment> toClose = new ArrayList<>(activeAssignmentsForCollege(college));
        toClose.addAll(activeAssignmentsForUser(replacement.getId()));
        Set<Long> outgoingIds = new LinkedHashSet<>();
        for (DeanAssignment assignment : toClose) {
            assignment.setStatus(DeanAssignment.STATUS_REVOKED);
            assignment.setDateRevoked(now);
            assignment.setRevokedBy(actor.getId());
            assignment.setRevokedByName(actor.getName());
            deanAssignmentRepository.save(assignment);
            outgoingIds.add(assignment.getUserId());
        }

        // 2. Demote the outgoing Deans that no longer hold any active appointment. The
        //    account is kept: only the Dean role and the Dean scope are removed.
        for (Long outgoingId : outgoingIds) {
            if (outgoingId.equals(replacement.getId())) {
                continue;
            }
            if (hasActiveAssignment(outgoingId)) {
                continue;
            }
            User outgoing = userRepository.findById(outgoingId).orElse(null);
            if (outgoing == null || !ROLE_DEAN.equalsIgnoreCase(outgoing.getRole())) {
                continue;
            }
            outgoing.setRole(ROLE_PROPONENT);
            outgoing.setAssignedCollege(null);
            outgoing.setDesignation(null);
            userRepository.save(outgoing);
            audit(actor, EndorsementRequestEvent.TYPE_DEAN_ROLE_REVOKED,
                    "Dean role revoked from " + outgoing.getName() + " (" + outgoing.getEmail()
                            + ") when " + replacement.getName() + " was appointed"
                            + (college == null ? " for the institution." : " for " + college + "."));
        }

        // 3. Activate the incoming Dean and open its appointment.
        replacement.setRole(ROLE_DEAN);
        replacement.setAssignedCollege(college);
        replacement.setDesignation(designation);
        User savedReplacement = userRepository.save(replacement);

        DeanAssignment active = new DeanAssignment();
        active.setUserId(savedReplacement.getId());
        active.setDeanName(savedReplacement.getName());
        active.setDeanEmail(savedReplacement.getEmail());
        active.setAssignedCollege(college);
        active.setDesignation(designation);
        active.setStatus(DeanAssignment.STATUS_ACTIVE);
        active.setDateAssigned(now);
        active.setCreatedBy(actor.getId());
        active.setCreatedByName(actor.getName());
        DeanAssignment savedAssignment = deanAssignmentRepository.save(active);

        // 4. Record the succession.
        activityLogService.success(ActivityLog.ACTION_ROLE_CHANGED, ActivityLog.MODULE_USER_ACCOUNTS,
                "Changed user role or permissions",
                "Dean role assigned to " + savedReplacement.getName() + " ("
                        + savedReplacement.getEmail() + ")" + scopeNote(savedReplacement)
                        + " after a change of leadership.");
        audit(actor, EndorsementRequestEvent.TYPE_DEAN_ROLE_REPLACED,
                "Dean of " + (college == null ? "the institution (no college assigned)" : college)
                        + " replaced: " + savedReplacement.getName() + " now holds the appointment as "
                        + (designation == null ? "Dean" : designation) + ".");
        return DeanAssignmentResponse.of(savedAssignment);
    }

    /**
     * Creates a Dean account, or assigns the Dean role (and scope) to an existing
     * account. The password of an existing account is never touched.
     */
    @Transactional
    public User save(DeanAccountRequest request, Long actorId, String actorRole) {
        requireAdministrator(actorRole);
        User actor = requireAccount(actorId);

        User dean;
        boolean created = false;
        if (request.getUserId() != null) {
            dean = requireAccount(request.getUserId());
            dean.setRole(ROLE_DEAN);
        } else {
            dean = createAccount(request);
            created = true;
        }

        String previousCollege = dean.getAssignedCollege();
        String previousDesignation = dean.getDesignation();

        if (request.getDesignation() != null) {
            dean.setDesignation(blankToNull(request.getDesignation()));
        }
        if (request.getAssignedCollege() != null) {
            dean.setAssignedCollege(blankToNull(request.getAssignedCollege()));
        }
        if (request.getStatus() != null) {
            String status = request.getStatus().trim().toUpperCase(Locale.ROOT);
            if (!ALLOWED_STATUSES.contains(status)) {
                throw new IllegalArgumentException(
                        "The account status must be APPROVED (active) or INACTIVE (deactivated).");
            }
            dean.setStatus(status);
        }

        User saved = userRepository.save(dean);

        // Keep the appointment history in step with the account. A college can never hold
        // two active Deans through this path; handing a college from one Dean to another
        // is the dedicated replace() workflow, so this refuses to shortcut it.
        syncActiveAssignment(saved, actor);

        activityLogService.success(
                created ? ActivityLog.ACTION_ACCOUNT_CREATED : ActivityLog.ACTION_ROLE_CHANGED,
                ActivityLog.MODULE_USER_ACCOUNTS,
                created ? "Created a new user account" : "Changed user role or permissions",
                (created ? "Dean account created for " : "Dean role assigned to existing account ")
                        + saved.getName() + " (" + saved.getEmail() + ")" + scopeNote(saved) + ".");

        audit(actor, EndorsementRequestEvent.TYPE_DEAN_ROLE_ASSIGNED,
                (created ? "Dean account created for " : "Dean role assigned to existing account ")
                        + saved.getName() + " (" + saved.getEmail() + ")" + scopeNote(saved) + ".");
        if (!Objects.equals(previousCollege, saved.getAssignedCollege())
                || !Objects.equals(previousDesignation, saved.getDesignation())) {
            audit(actor, EndorsementRequestEvent.TYPE_DEAN_SCOPE_CHANGED,
                    "Organizational assignment of " + saved.getName() + " changed from "
                            + describe(previousDesignation, previousCollege) + " to "
                            + describe(saved.getDesignation(), saved.getAssignedCollege()) + ".");
        }
        return saved;
    }

    /**
     * Revokes the Dean role: the account keeps working as a Faculty Proponent and
     * loses the endorsement scope. The account itself is never deleted.
     */
    @Transactional
    public User revoke(Long userId, Long actorId, String actorRole) {
        requireAdministrator(actorRole);
        User actor = requireAccount(actorId);
        User dean = requireAccount(userId);
        if (!ROLE_DEAN.equalsIgnoreCase(dean.getRole())) {
            throw new IllegalArgumentException("That account does not hold the Dean role.");
        }
        String previous = dean.getAssignedCollege();
        dean.setRole(ROLE_PROPONENT);
        dean.setAssignedCollege(null);
        dean.setDesignation(null);
        User saved = userRepository.save(dean);
        revokeActiveAssignments(saved.getId(), actor);
        activityLogService.success(ActivityLog.ACTION_ROLE_CHANGED,
                ActivityLog.MODULE_USER_ACCOUNTS, "Changed user role or permissions",
                "The Dean role was revoked from " + saved.getName() + " (" + saved.getEmail()
                        + "); the account is a Faculty Proponent again.");

        audit(actor, EndorsementRequestEvent.TYPE_DEAN_ROLE_REVOKED,
                "Dean role revoked from " + saved.getName() + " (" + saved.getEmail() + ")"
                        + (previous == null || previous.isBlank() ? "" : "; former scope: " + previous)
                        + ". The account keeps working as a Faculty Proponent.");
        return saved;
    }

    /**
     * Brings the account's ACTIVE appointment in line with the account after a create or a
     * scope update. Refuses to create a second active Dean for a college (that is what
     * {@link #replaceDean} is for) and closes the appointment of a deactivated account.
     */
    private void syncActiveAssignment(User dean, User actor) {
        if (!STATUS_APPROVED.equalsIgnoreCase(dean.getStatus())) {
            revokeActiveAssignments(dean.getId(), actor);
            return;
        }

        String college = dean.getAssignedCollege();
        for (DeanAssignment other : activeAssignmentsForCollege(college)) {
            if (!other.getUserId().equals(dean.getId())) {
                throw new IllegalArgumentException(
                        "That college already has an active Dean. Use Manage Dean Role to replace them.");
            }
        }

        DeanAssignment mine = activeAssignmentForUser(dean.getId());
        if (mine == null) {
            mine = new DeanAssignment();
            mine.setUserId(dean.getId());
            mine.setStatus(DeanAssignment.STATUS_ACTIVE);
            mine.setDateAssigned(LocalDateTime.now());
            mine.setCreatedBy(actor != null ? actor.getId() : null);
            mine.setCreatedByName(actor != null ? actor.getName() : "System");
        }
        mine.setDeanName(dean.getName());
        mine.setDeanEmail(dean.getEmail());
        mine.setAssignedCollege(college);
        mine.setDesignation(dean.getDesignation());
        deanAssignmentRepository.save(mine);
    }

    /** Closes every ACTIVE appointment of an account, keeping each row as history. */
    private void revokeActiveAssignments(Long userId, User actor) {
        for (DeanAssignment assignment : activeAssignmentsForUser(userId)) {
            assignment.setStatus(DeanAssignment.STATUS_REVOKED);
            assignment.setDateRevoked(LocalDateTime.now());
            if (actor != null) {
                assignment.setRevokedBy(actor.getId());
                assignment.setRevokedByName(actor.getName());
            }
            deanAssignmentRepository.save(assignment);
        }
    }

    private List<DeanAssignment> activeAssignmentsForCollege(String college) {
        if (college == null) {
            return deanAssignmentRepository.findByStatusAndAssignedCollegeIsNull(DeanAssignment.STATUS_ACTIVE);
        }
        return deanAssignmentRepository
                .findByStatusAndAssignedCollegeIgnoreCase(DeanAssignment.STATUS_ACTIVE, college);
    }

    private List<DeanAssignment> activeAssignmentsForUser(Long userId) {
        return deanAssignmentRepository.findByUserIdAndStatus(userId, DeanAssignment.STATUS_ACTIVE);
    }

    private DeanAssignment activeAssignmentForUser(Long userId) {
        List<DeanAssignment> rows = activeAssignmentsForUser(userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private boolean hasActiveAssignment(Long userId) {
        return deanAssignmentRepository.countByUserIdAndStatus(userId, DeanAssignment.STATUS_ACTIVE) > 0;
    }

    private User createAccount(DeanAccountRequest request) {
        String name = blankToNull(request.getName());
        if (name == null) {
            throw new IllegalArgumentException("The Dean's full name is required.");
        }
        String email = blankToNull(request.getEmail());
        if (email == null) {
            throw new IllegalArgumentException("The Dean's institutional email is required.");
        }
        if (!InstitutionalCredentialsService.isInstitutionalAddress(email)) {
            throw new IllegalArgumentException("Only MSU Naawan email addresses are allowed.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException(
                    "An account already exists for that email address. Use a different "
                            + "institutional email to create a new Dean account.");
        }
        String password = blankToNull(request.getPassword());
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException(
                    "Choose an initial password of at least 6 characters for the Dean account.");
        }

        User dean = new User();
        dean.setName(name);
        dean.setEmail(email);
        // The administrator-chosen initial password is hashed before it is stored, so
        // this path cannot put a readable password in the column either.
        dean.setPassword(passwordService.hash(password));
        dean.setRole(ROLE_DEAN);
        dean.setStatus(STATUS_APPROVED);
        dean.setEmailVerified(true);
        dean.setDateRegistered(LocalDate.now());
        // The initial password is one an administrator chose or the system generated, never
        // one the Dean picked, so the account must change it at the first sign-in. The flag is
        // part of the INSERT: setting it only after the account exists would leave the window
        // in which the account is usable with the temporary password.
        dean.setMustChangePassword(true);
        return dean;
    }

    private void requireAdministrator(String actorRole) {
        if (!EndorsementRequestService.ROLE_ADMIN.equalsIgnoreCase(actorRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only an RPS administrator may manage Dean accounts.");
        }
    }

    private User requireAccount(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Account not found."));
    }

    /** Writes an account-level audit entry (no endorsement request involved). */
    private void audit(User actor, String eventType, String detail) {
        EndorsementRequestEvent event = new EndorsementRequestEvent();
        event.setEndorsementRequestId(null);
        event.setEventType(eventType);
        if (actor != null) {
            event.setActorId(actor.getId());
            event.setActorName(actor.getName());
            event.setActorRole(actor.getRole());
        }
        event.setDetail(detail);
        eventRepository.save(event);
    }

    private static String scopeNote(User dean) {
        String college = dean.getAssignedCollege();
        if (college == null || college.isBlank()) {
            return " with no college assigned yet (institution-wide fallback)";
        }
        return " for " + college;
    }

    private static String describe(String designation, String college) {
        String left = designation == null || designation.isBlank() ? "no designation" : designation;
        String right = college == null || college.isBlank() ? "no college" : college;
        return left + " / " + right;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
