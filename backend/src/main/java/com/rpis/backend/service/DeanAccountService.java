package com.rpis.backend.service;

import com.rpis.backend.dto.DeanAccountRequest;
import com.rpis.backend.model.EndorsementRequestEvent;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.EndorsementRequestEventRepository;
import com.rpis.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
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

    /** The Dean / Department Chair role identifier. */
    public static final String ROLE_DEAN = "RPS_DEAN";

    /** The role an account is returned to when the Dean role is revoked. */
    public static final String ROLE_PROPONENT = "PROPONENT";

    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_INACTIVE = "INACTIVE";
    private static final Set<String> ALLOWED_STATUSES = Set.of(STATUS_APPROVED, STATUS_INACTIVE);

    private static final String INSTITUTIONAL_DOMAIN = "@msunaawan.edu.ph";

    private final UserRepository userRepository;
    private final EndorsementRequestEventRepository eventRepository;

    /** Every Dean account, by name. */
    public List<User> listDeans() {
        return userRepository.findByRoleOrderByNameAsc(ROLE_DEAN);
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
        audit(actor, EndorsementRequestEvent.TYPE_DEAN_ROLE_REVOKED,
                "Dean role revoked from " + saved.getName() + " (" + saved.getEmail() + ")"
                        + (previous == null || previous.isBlank() ? "" : "; former scope: " + previous)
                        + ". The account keeps working as a Faculty Proponent.");
        return saved;
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
        if (!email.toLowerCase(Locale.ROOT).endsWith(INSTITUTIONAL_DOMAIN)) {
            throw new IllegalArgumentException("Only MSU Naawan email addresses are allowed.");
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException(
                    "An account already exists for that email. Assign the Dean role to it instead "
                            + "of creating a second account.");
        }
        String password = blankToNull(request.getPassword());
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException(
                    "Choose an initial password of at least 6 characters for the Dean account.");
        }

        User dean = new User();
        dean.setName(name);
        dean.setEmail(email);
        dean.setPassword(password);
        dean.setRole(ROLE_DEAN);
        dean.setStatus(STATUS_APPROVED);
        dean.setEmailVerified(true);
        dean.setDateRegistered(LocalDate.now());
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
