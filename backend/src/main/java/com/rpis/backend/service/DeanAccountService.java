package com.rpis.backend.service;

import com.rpis.backend.dto.DeanAccountRequest;
import com.rpis.backend.dto.DeanAssignmentResponse;
import com.rpis.backend.dto.DeanReplacementCreateRequest;
import com.rpis.backend.dto.DeanReplacementRequest;
import com.rpis.backend.model.ActivityLog;
import com.rpis.backend.model.DeanAssignment;
import com.rpis.backend.model.EndorsementRequest;
import com.rpis.backend.model.EndorsementRequestEvent;
import com.rpis.backend.model.User;
import com.rpis.backend.model.UserRoles;
import com.rpis.backend.repository.DeanAssignmentRepository;
import com.rpis.backend.repository.EndorsementRequestEventRepository;
import com.rpis.backend.repository.EndorsementRequestRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.util.EmailNormalizer;
import com.rpis.backend.util.NotificationTypes;
import com.rpis.backend.util.PersonNameFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final Set<String> ALLOWED_STATUSES = Set.of(STATUS_APPROVED, STATUS_INACTIVE);

    /**
     * The result of provisioning a Dean: the account (created or reused), whether an
     * existing account was reused, and the credentials that must be shown once when a new
     * account was created. A reused account keeps its password: the response carries no
     * temporary password because the person already has one.
     */
    public record ProvisionedDean(User user, boolean reused, String email,
            String temporaryPassword, boolean emailAdjusted) {
    }

    private final UserRepository userRepository;
    private final EndorsementRequestEventRepository eventRepository;
    private final DeanAssignmentRepository deanAssignmentRepository;
    private final EndorsementRequestRepository endorsementRequestRepository;
    private final PasswordService passwordService;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    /**
     * Fills the read-only {@code deanStatus} and the historical Dean role on an account
     * list. The appointment table stays the only source of truth:
     * <ul>
     *   <li>an active appointment reports {@code ACTIVE};</li>
     *   <li>a closed appointment reports {@code REVOKED}, and the account is shown with a
     *       <em>Dean (Revoked)</em> role even when the active Dean role has been removed.
     *       No Proponent role is invented for it;</li>
     *   <li>an account with no Dean appointment at all keeps no Dean status.</li>
     * </ul>
     * Nothing is written to the account.
     */
    public void attachDeanStatuses(List<User> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        Set<Long> active = new HashSet<>();
        Set<Long> history = new HashSet<>();
        for (DeanAssignment assignment : deanAssignmentRepository.findAllByOrderByDateAssignedDesc()) {
            if (assignment.getUserId() == null) {
                continue;
            }
            history.add(assignment.getUserId());
            if (DeanAssignment.STATUS_ACTIVE.equalsIgnoreCase(assignment.getStatus())) {
                active.add(assignment.getUserId());
            }
        }
        for (User user : users) {
            if (user == null || user.getId() == null) {
                continue;
            }
            if (active.contains(user.getId())) {
                user.setDeanStatus(DeanAssignment.STATUS_ACTIVE);
                continue;
            }
            if (history.contains(user.getId()) || user.hasRole(ROLE_DEAN)) {
                user.setDeanStatus(DeanAssignment.STATUS_REVOKED);
                if (!user.hasRole(ROLE_DEAN)) {
                    user.setRevokedRoles(new ArrayList<>(List.of(ROLE_DEAN)));
                }
            }
        }
    }

    /**
     * Every Dean account, by name. The role is read from the assignment table as well as
     * the primary column, so a Proponent who was also appointed Dean is listed here.
     */
    public List<User> listDeans() {
        return userRepository.findByAssignedRoleOrderByNameAsc(ROLE_DEAN);
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
        // The account is the source of truth for the person's current name and address, so
        // both screens show the same values after a rename. The assignment keeps the
        // snapshot as the fallback for a row whose account cannot be found.
        Map<Long, User> accounts = new HashMap<>();
        List<Long> userIds = rows.stream()
                .map(DeanAssignment::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!userIds.isEmpty()) {
            for (User user : userRepository.findAllById(userIds)) {
                accounts.put(user.getId(), user);
            }
        }
        return rows.stream()
                .map(row -> DeanAssignmentResponse.of(row, accounts.get(row.getUserId())))
                .toList();
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
                && user.hasRole(ROLE_DEAN)
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
     *   <li>an outgoing Dean who no longer holds any active appointment loses the active
     *       Dean role (any other role they hold is kept) - their account, its records and the closed appointment
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

        if (request == null || request.getAssignmentId() == null) {
            throw new IllegalArgumentException(
                    "Open the replacement from the Dean appointment being replaced.");
        }
        if (request.getUserId() == null) {
            throw new IllegalArgumentException("Select the account that will be the new Dean.");
        }
        DeanAssignment outgoingAssignment = deanAssignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "That Dean appointment no longer exists."));
        if (!DeanAssignment.STATUS_ACTIVE.equalsIgnoreCase(outgoingAssignment.getStatus())) {
            throw new IllegalArgumentException(
                    "Only an active Dean appointment can be replaced.");
        }
        // College and designation come from the stored appointment, never from the
        // browser: the Dean management screen is not allowed to change either field.
        String college = blankToNull(outgoingAssignment.getAssignedCollege());
        String designation = blankToNull(outgoingAssignment.getDesignation());

        User replacement = requireAccount(request.getUserId());

        // The outgoing Dean of this unit is never the replacement.
        Long currentDeanId = outgoingAssignment.getUserId();
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

        // The succession itself is shared with the create-and-replace workflow below, so
        // there is exactly one implementation of "close the outgoing appointment, open the
        // incoming one".
        DeanAssignment savedAssignment = appointDean(actor, replacement, college, designation);
        return DeanAssignmentResponse.of(savedAssignment, replacement);
    }

    /**
     * Creates the incoming Dean for an existing ACTIVE appointment and completes the
     * replacement, all in the caller's transaction.
     *
     * <p>This is the Create Dean Account step of the Replace Dean workflow: the account is
     * provisioned exactly as any other Dean account (generated institutional address and
     * temporary password, hashed password, audit entry), then the sitting appointment is
     * closed as history and the new account becomes the sitting Dean. Doing it here, rather
     * than calling the account-creation endpoint and the replacement endpoint in sequence,
     * means a college can never end up with two active Deans or none because the second call
     * failed.
     *
     * <p>The college is the appointment's college, never the browser's choice: a request that
     * names another college is refused before anything is written.
     */
    @Transactional
    public ProvisionedDean createReplacementDean(DeanReplacementCreateRequest request,
            Long actorId, String actorRole) {
        requireAdministrator(actorRole);
        User actor = requireAccount(actorId);

        if (request == null || request.getAssignmentId() == null) {
            throw new IllegalArgumentException(
                    "Open the replacement from the Dean appointment being replaced.");
        }
        DeanAssignment outgoingAssignment = deanAssignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "That Dean appointment no longer exists."));
        if (!DeanAssignment.STATUS_ACTIVE.equalsIgnoreCase(outgoingAssignment.getStatus())) {
            throw new IllegalArgumentException(
                    "Only an active Dean appointment can be replaced.");
        }
        // College and designation come from the stored appointment; the browser may send the
        // locked college but it must agree with the appointment.
        String college = blankToNull(outgoingAssignment.getAssignedCollege());
        String requestedCollege = blankToNull(request.getAssignedCollege());
        if (requestedCollege != null && !sameCollege(requestedCollege, college)) {
            throw new IllegalArgumentException(
                    "The replacement Dean must be appointed to the same college as the sitting Dean.");
        }
        String designation = blankToNull(request.getDesignation());
        if (designation == null) {
            designation = blankToNull(outgoingAssignment.getDesignation());
        }

        String name = blankToNull(request.getName());
        if (name == null) {
            throw new IllegalArgumentException("The new Dean's full name is required.");
        }
        name = PersonNameFormatter.format(name);

        // A person who already has an account (typically the Proponent with this
        // institutional address) is reused rather than duplicated: the Dean role is added
        // to that account and its address and password are left untouched.
        String baseEmail = InstitutionalCredentialsService.emailFor(name);
        User existing = reusableAccountFor(name, baseEmail);
        User incoming;
        boolean reused = existing != null;
        String email;
        String temporaryPassword = null;
        if (reused) {
            incoming = existing;
            incoming.assignRole(ROLE_DEAN);
            email = incoming.getEmail();
        } else {
            email = baseEmail;
            temporaryPassword = InstitutionalCredentialsService.temporaryPasswordFor(name);
            DeanAccountRequest accountRequest = new DeanAccountRequest();
            accountRequest.setName(name);
            accountRequest.setEmail(email);
            accountRequest.setPassword(temporaryPassword);
            accountRequest.setDesignation(designation);
            accountRequest.setAssignedCollege(college);
            // The account is created (not yet persisted) with its own validation, hashed
            // password and first-login password change; appointDean saves it and opens the
            // appointment in the same transaction.
            incoming = createAccount(accountRequest);
        }

        appointDean(actor, incoming, college, designation);
        return new ProvisionedDean(incoming, reused, email, temporaryPassword, false);
    }

    /**
     * Applies a change of leadership: closes every ACTIVE appointment of the college and any
     * ACTIVE appointment the incoming Dean already holds, returns the outgoing Deans who are
     * left without an appointment lose only the Dean role (other roles are kept), and opens the incoming Dean's ACTIVE
     * appointment. Accounts and closed appointment rows are never deleted.
     */
    private DeanAssignment appointDean(User actor, User incoming, String college,
            String designation) {
        LocalDateTime now = LocalDateTime.now();

        // 1. Close the outgoing appointment(s): the sitting Dean of this college, plus any
        //    other active appointment the incoming Dean already holds (nobody holds two).
        List<DeanAssignment> toClose = new ArrayList<>(activeAssignmentsForCollege(college));
        if (incoming.getId() != null) {
            toClose.addAll(activeAssignmentsForUser(incoming.getId()));
        }
        Set<Long> outgoingIds = new LinkedHashSet<>();
        for (DeanAssignment assignment : toClose) {
            assignment.setStatus(DeanAssignment.STATUS_REVOKED);
            assignment.setDateRevoked(now);
            assignment.setRevokedBy(actor.getId());
            assignment.setRevokedByName(actor.getName());
            deanAssignmentRepository.save(assignment);
            outgoingIds.add(assignment.getUserId());
        }
        // The partial unique index ux_dean_assignments_active_college allows only one
        // ACTIVE row per college (including the institution-wide '' key). Hibernate flushes
        // INSERTs before UPDATEs, so the new ACTIVE appointment below would be inserted
        // while the outgoing row still looks ACTIVE and the index would reject it. Flush
        // the closes first; they are still part of this transaction and roll back with it.
        deanAssignmentRepository.flush();

        // 2. Activate the incoming Dean and open its appointment, so the transfer below
        //    has a saved account to point the pending requests at. An existing account keeps
        //    its primary role and every other assigned role; the Dean role is added.
        incoming.assignRole(ROLE_DEAN);
        incoming.setAssignedCollege(college);
        incoming.setDesignation(designation);
        User savedIncoming = userRepository.save(incoming);

        DeanAssignment active = new DeanAssignment();
        active.setUserId(savedIncoming.getId());
        active.setDeanName(savedIncoming.getName());
        active.setDeanEmail(savedIncoming.getEmail());
        active.setAssignedCollege(college);
        active.setDesignation(designation);
        active.setStatus(DeanAssignment.STATUS_ACTIVE);
        active.setDateAssigned(now);
        active.setCreatedBy(actor.getId());
        active.setCreatedByName(actor.getName());
        DeanAssignment savedAssignment = deanAssignmentRepository.save(active);

        // 3. Transfer the outgoing Dean's pending endorsement requests to the incoming
        //    Dean BEFORE the outgoing role is removed, inside this same transaction. A
        //    failure here therefore rolls the whole succession back and leaves the
        //    outgoing Dean (and their requests) fully intact.
        int transferredRequests = transferPendingEndorsements(outgoingIds, savedIncoming);

        // 4. Demote the outgoing Deans that no longer hold any active appointment. The
        //    account is kept: only the Dean role and the Dean scope are removed.
        List<User> demoted = new ArrayList<>();
        for (Long outgoingId : outgoingIds) {
            if (incoming.getId() != null && incoming.getId().equals(outgoingId)) {
                continue;
            }
            if (hasActiveAssignment(outgoingId)) {
                continue;
            }
            User outgoing = userRepository.findById(outgoingId).orElse(null);
            if (outgoing == null || !outgoing.hasRole(ROLE_DEAN)) {
                continue;
            }
            demoteFromDean(outgoing);
            userRepository.save(outgoing);
            demoted.add(outgoing);
            audit(actor, EndorsementRequestEvent.TYPE_DEAN_ROLE_REVOKED,
                    "Dean role revoked from " + outgoing.getName() + " (" + outgoing.getEmail()
                            + ") when " + incoming.getName() + " was appointed"
                            + (college == null ? " for the institution." : " for " + college + ".")
                            + " Pending endorsement requests transferred: " + transferredRequests + ".");
        }

        // 5. Record the succession.
        activityLogService.success(ActivityLog.ACTION_DEAN_REPLACED,
                ActivityLog.MODULE_DEAN_ROLE_MANAGEMENT,
                "Replaced the Dean of " + (college == null ? "the institution" : college),
                "Dean role assigned to " + savedIncoming.getName() + " ("
                        + savedIncoming.getEmail() + ")" + scopeNote(savedIncoming)
                        + " after a change of leadership. Pending endorsement requests transferred: "
                        + transferredRequests + ".");
        audit(actor, EndorsementRequestEvent.TYPE_DEAN_ROLE_REPLACED,
                "Dean of " + (college == null ? "the institution (no college assigned)" : college)
                        + " replaced: " + savedIncoming.getName() + " now holds the appointment as "
                        + (designation == null ? "Dean" : designation) + ". "
                        + "Pending endorsement requests transferred to the new Dean: "
                        + transferredRequests + ".");
        String appointmentMessage = "You have been appointed as Dean"
                + scopeNote(savedIncoming) + ".";
        if (transferredRequests > 0) {
            appointmentMessage += " " + transferredRequests
                    + (transferredRequests == 1
                            ? " pending endorsement request was transferred to you."
                            : " pending endorsement requests were transferred to you.");
        }
        notifyDeanAssignment(savedIncoming, "Dean Role Assigned", appointmentMessage);
        for (User outgoing : demoted) {
            notifyDeanAssignment(outgoing, "Dean Role Revoked",
                    "Your Dean role has been revoked following a change of leadership. "
                            + "Any other role on your account is unchanged.");
        }
        return savedAssignment;
    }

    /**
     * Creates a Dean account, or assigns the Dean role (and scope) to an existing account.
     *
     * <p>An address that already belongs to the same person - typically the Proponent
     * account with this institutional address - is not duplicated and never receives a
     * numbered variant: that account is reused, and the Dean role is added beside the role
     * it already holds. The password of an existing account is never touched.
     */
    @Transactional
    public ProvisionedDean save(DeanAccountRequest request, Long actorId, String actorRole) {
        requireAdministrator(actorRole);
        User actor = requireAccount(actorId);

        User dean;
        boolean created = false;
        boolean reused = false;
        String email;
        String temporaryPassword = null;
        if (request.getUserId() != null) {
            dean = requireAccount(request.getUserId());
            dean.assignRole(ROLE_DEAN);
            // Dean Role Management exposes the full name for editing. When it is sent,
            // it is stored in the one RPIS format; the institutional address is not
            // re-derived, so the account keeps the address it was created with.
            if (request.getName() != null && !request.getName().isBlank()) {
                dean.setName(PersonNameFormatter.format(request.getName()));
            }
            reused = true;
            email = dean.getEmail();
        } else {
            String name = blankToNull(request.getName());
            if (name == null) {
                throw new IllegalArgumentException("The Dean's full name is required.");
            }
            name = PersonNameFormatter.format(name);
            String baseEmail = InstitutionalCredentialsService.emailFor(name);
            User existing = reusableAccountFor(name, baseEmail);
            if (existing != null) {
                // Reuse the account as it stands: its name, address and password are the
                // person's existing account data and are not rewritten by the promotion.
                dean = existing;
                dean.assignRole(ROLE_DEAN);
                reused = true;
                email = dean.getEmail();
            } else {
                request.setName(name);
                request.setEmail(baseEmail);
                request.setPassword(InstitutionalCredentialsService.temporaryPasswordFor(name));
                temporaryPassword = request.getPassword();
                dean = createAccount(request);
                created = true;
                email = baseEmail;
            }
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
                ActivityLog.ACTION_DEAN_ASSIGNED,
                ActivityLog.MODULE_DEAN_ROLE_MANAGEMENT,
                created
                        ? "Created a Dean account"
                                + (saved.getAssignedCollege() == null
                                        ? " (institution-wide)"
                                        : " for " + saved.getAssignedCollege())
                        : "Assigned the Dean role to " + saved.getName(),
                (created ? "Dean account created for " : "Dean role assigned to existing account ")
                        + saved.getName() + " (" + saved.getEmail() + ")" + scopeNote(saved) + ".");

        audit(actor, EndorsementRequestEvent.TYPE_DEAN_ROLE_ASSIGNED,
                (created ? "Dean account created for " : "Dean role assigned to existing account ")
                        + saved.getName() + " (" + saved.getEmail() + ")" + scopeNote(saved) + ".");
        notifyDeanAssignment(saved, "Dean Role Assigned",
                "Your account now holds the Dean role" + scopeNote(saved) + ".");
        if (!Objects.equals(previousCollege, saved.getAssignedCollege())
                || !Objects.equals(previousDesignation, saved.getDesignation())) {
            audit(actor, EndorsementRequestEvent.TYPE_DEAN_SCOPE_CHANGED,
                    "Organizational assignment of " + saved.getName() + " changed from "
                            + describe(previousDesignation, previousCollege) + " to "
                            + describe(saved.getDesignation(), saved.getAssignedCollege()) + ".");
        }
        return new ProvisionedDean(saved, reused, email, temporaryPassword, false);
    }

    /**
     * Revokes the Dean role: the account loses the endorsement scope and the active Dean
     * role, while every other role it holds is preserved. The account itself is never
     * deleted, and the closed appointment remains visible as history.
     *
     * <p>A Dean appointed to a college cannot be revoked alone. The sitting Dean owns the
     * college's pending endorsement requests, so the administrator must use Replace Dean,
     * which appoints a replacement to the same college and transfers those requests in
     * the same transaction. Only an institution-wide Dean - or a college that already has
     * a different sitting Dean - can be revoked directly.</p>
     */
    @Transactional
    public User revoke(Long userId, Long actorId, String actorRole) {
        requireAdministrator(actorRole);
        User actor = requireAccount(actorId);
        User dean = requireAccount(userId);
        if (!dean.hasRole(ROLE_DEAN)) {
            throw new IllegalArgumentException("That account does not hold the Dean role.");
        }
        if (!hasActiveAssignment(userId)) {
            throw new IllegalArgumentException(
                    "That account has no active Dean appointment to revoke.");
        }
        String college = blankToNull(dean.getAssignedCollege());
        if (college != null && !hasOtherActiveDeanForCollege(college, userId)) {
            throw new IllegalArgumentException(
                    "This Dean cannot be revoked yet. Please assign a replacement Dean to this "
                            + "college or department first to ensure that pending endorsement "
                            + "requests and responsibilities are properly transferred.");
        }
        String previous = college;
        demoteFromDean(dean);
        User saved = userRepository.save(dean);
        revokeActiveAssignments(saved.getId(), actor);
        activityLogService.success(ActivityLog.ACTION_DEAN_REVOKED,
                ActivityLog.MODULE_DEAN_ROLE_MANAGEMENT,
                "Revoked the Dean role from " + saved.getName(),
                "The Dean role was revoked from " + saved.getName() + " (" + saved.getEmail()
                        + "); the active Dean role was removed and other roles are unchanged.");

        audit(actor, EndorsementRequestEvent.TYPE_DEAN_ROLE_REVOKED,
                "Dean role revoked from " + saved.getName() + " (" + saved.getEmail() + ")"
                        + (previous == null || previous.isBlank() ? "" : "; former scope: " + previous)
                        + ". The active Dean role was removed; other roles are unchanged.");
        notifyDeanAssignment(saved, "Dean Role Revoked",
                "Your Dean role has been revoked. Any other role on your account is unchanged.");
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
                        "That college already has an active Dean. Use Manage Dean to replace them.");
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

    /**
     * Reassigns every PENDING endorsement request routed to an outgoing Dean to the
     * incoming Dean. Only the owning reference changes: the status, submission data,
     * decision history and proposal link are untouched, so no request is lost, duplicated
     * or reset.
     *
     * @return how many requests were transferred
     */
    private int transferPendingEndorsements(Set<Long> outgoingIds, User incoming) {
        if (incoming == null || incoming.getId() == null) {
            return 0;
        }
        int transferred = 0;
        for (Long outgoingId : outgoingIds) {
            if (outgoingId == null || outgoingId.equals(incoming.getId())) {
                continue;
            }
            for (EndorsementRequest request : endorsementRequestRepository
                    .findByAssignedDeanIdAndStatus(outgoingId, EndorsementRequest.STATUS_PENDING)) {
                request.setAssignedDean(incoming);
                endorsementRequestRepository.save(request);
                transferred++;
            }
        }
        return transferred;
    }

    /** True when a college already has a different sitting Dean (a replacement exists). */
    private boolean hasOtherActiveDeanForCollege(String college, Long userId) {
        for (DeanAssignment assignment : activeAssignmentsForCollege(college)) {
            if (!userId.equals(assignment.getUserId())) {
                return true;
            }
        }
        return false;
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

    /**
     * The existing account that this generated address identifies, or null when the
     * address is free. Matching ignores case and surrounding space.
     *
     * <p>If the address exists but the account's own name derives a different local part,
     * the address was assigned to somebody else; that is reported as a conflict instead of
     * silently merging two people. A non-active account is likewise reported rather than
     * gaining a role that its status would not let it use.
     */
    private User reusableAccountFor(String formattedName, String baseEmail) {
        String normalized = EmailNormalizer.normalize(baseEmail);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        User existing = userRepository.findByNormalizedEmail(normalized).orElse(null);
        if (existing == null) {
            return null;
        }
        String requestedLocal = InstitutionalCredentialsService.localPart(formattedName);
        String existingLocal = InstitutionalCredentialsService.localPart(existing.getName());
        if (!requestedLocal.equalsIgnoreCase(existingLocal)) {
            throw new IllegalArgumentException(
                    "The institutional email " + existing.getEmail()
                            + " already belongs to " + existing.getName()
                            + ". Verify the name, or manage that account from User Accounts.");
        }
        if (!STATUS_APPROVED.equalsIgnoreCase(existing.getStatus())) {
            throw new IllegalArgumentException(
                    "The account with this institutional email is not active ("
                            + existing.getStatus()
                            + "). Approve it before assigning the Dean role.");
        }
        return existing;
    }

    /**
     * Removes the active Dean role from an account while preserving every other role it
     * holds. No other role is invented: an account that only ever held the Dean role has
     * no active system role after this, and its revoked Dean appointment remains visible
     * as history in User Accounts. The primary {@code role} column is left as it is, so
     * the record still says the account was created as a Dean.
     */
    private void demoteFromDean(User user) {
        user.removeRole(ROLE_DEAN);
        user.setAssignedCollege(null);
        user.setDesignation(null);
    }

    private User createAccount(DeanAccountRequest request) {
        String name = blankToNull(request.getName());
        if (name == null) {
            throw new IllegalArgumentException("The Dean's full name is required.");
        }
        name = PersonNameFormatter.format(name);
        String email = EmailNormalizer.normalize(blankToNull(request.getEmail()));
        if (email == null) {
            throw new IllegalArgumentException("The Dean's institutional email is required.");
        }
        if (!InstitutionalCredentialsService.isInstitutionalAddress(email)) {
            throw new IllegalArgumentException("Only MSU Naawan email addresses are allowed.");
        }
        if (userRepository.findByNormalizedEmail(email).isPresent()) {
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
        dean.assignRole(ROLE_DEAN);
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

    /**
     * Tells the affected account about a Dean-role change. The notification has no
     * proposal reference: it opens the account's profile, where the role and scope are
     * shown. It is written in the same transaction as the account change, so a failed
     * assignment never leaves a misleading message behind.
     */
    private void notifyDeanAssignment(User dean, String title, String message) {
        if (dean == null || dean.getId() == null) {
            return;
        }
        notificationService.notifyUsers(List.of(dean), message, title,
                NotificationTypes.DEAN_ASSIGNMENT, null, null, null, ROLE_DEAN);
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

    /** College names are compared the way the database does: without case. */
    private static boolean sameCollege(String left, String right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.equalsIgnoreCase(right);
    }
}
