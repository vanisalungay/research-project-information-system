package com.rpis.backend.service;

import com.rpis.backend.dto.ActivityLogPageResponse;
import com.rpis.backend.model.ActivityLog;
import com.rpis.backend.model.User;
import com.rpis.backend.repository.ActivityLogRepository;
import com.rpis.backend.repository.UserRepository;
import com.rpis.backend.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The activity history of the signed-in administrator.
 *
 * <p>Two responsibilities, both keyed to the authenticated principal:
 *
 * <ul>
 *   <li><b>Recording.</b> {@link #success} / {@link #failed} are called by the
 *       business services after an operation has actually happened. The actor is
 *       read from the Spring Security context - never from a request body - so an
 *       entry can only ever name the caller who really performed the action.</li>
 *   <li><b>Reading.</b> {@link #page} and {@link #find} are scoped to that same
 *       principal, which is what stops one administrator seeing another's
 *       history. No query in this class can return somebody else's rows.</li>
 * </ul>
 *
 * <p>Recording never breaks the operation it describes: the insert runs in its own
 * transaction and every failure is caught and logged. The alternative - joining the
 * caller's transaction - would turn a full disk or a lock timeout into a failed
 * account creation, and a database that refuses audit rows must not become a
 * database that refuses work.
 *
 * <p>Nothing sensitive is ever recorded. Callers pass the account or object name
 * and the change that was made; a password, a temporary password, a token and a
 * signature are not parameters of any method here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityLogService {

    /** One page of the table, when the caller does not ask for a size. */
    static final int DEFAULT_PAGE_SIZE = 10;

    /** The largest page a caller may ask for, so one request cannot pull the whole history. */
    static final int MAX_PAGE_SIZE = 100;

    private final ActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;
    private final CurrentUser currentUser;

    /** Records a completed action for the authenticated caller. */
    @Transactional
    public void success(String action, String module, String description, String details) {
        record(action, module, description, details, ActivityLog.STATUS_SUCCESS);
    }

    /** Records an action that was attempted and failed, where that is worth keeping. */
    @Transactional
    public void failed(String action, String module, String description, String details) {
        record(action, module, description, details, ActivityLog.STATUS_FAILED);
    }

    private void record(String action, String module, String description, String details,
            String status) {
        try {
            Long actorId = currentUser.getUserId();
            if (actorId == null) {
                // Background or seeding work with no signed-in actor: there is nobody to
                // attribute it to, and a nameless entry would be misleading.
                return;
            }
            ActivityLog entry = new ActivityLog();
            entry.setUserId(actorId);
            entry.setUserName(userRepository.findById(actorId).map(User::getName).orElse(null));
            entry.setAction(action);
            entry.setModule(module);
            entry.setDescription(description);
            entry.setDetails(details);
            entry.setStatus(status == null ? ActivityLog.STATUS_SUCCESS : status);
            activityLogRepository.save(entry);
        } catch (Exception e) {
            // Deliberately swallowed: an audit write must never fail the work it records.
            log.warn("Could not record the activity '{}' ({}): {}", action, module, e.getMessage());
        }
    }

    /**
     * One page of the caller's own history, newest first.
     *
     * @param search free text matched against the description and the module
     * @param module exact module name, or {@code null} for every module
     * @param status {@code SUCCESS} / {@code FAILED}, or {@code null} for both
     * @param from   inclusive start date, or {@code null}
     * @param to     inclusive end date, or {@code null}
     */
    @Transactional(readOnly = true)
    public ActivityLogPageResponse page(String search, String module, String status,
            LocalDate from, LocalDate to, Integer page, Integer size) {
        Long actorId = currentUser.requireUserId();
        Pageable pageable = PageRequest.of(page == null || page < 0 ? 0 : page, clampSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        Specification<ActivityLog> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            // The scope is not a filter the caller can drop: it is always the first term.
            predicates.add(builder.equal(root.get("userId"), actorId));

            String term = trimToNull(search);
            if (term != null) {
                String pattern = "%" + term.toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("description")), pattern),
                        builder.like(builder.lower(root.get("module")), pattern)));
            }
            String moduleFilter = trimToNull(module);
            if (moduleFilter != null) {
                predicates.add(builder.equal(builder.lower(root.get("module")),
                        moduleFilter.toLowerCase(Locale.ROOT)));
            }
            String statusFilter = trimToNull(status);
            if (statusFilter != null) {
                predicates.add(builder.equal(builder.upper(root.get("status")),
                        statusFilter.toUpperCase(Locale.ROOT)));
            }
            if (from != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"),
                        from.atStartOfDay()));
            }
            if (to != null) {
                // Inclusive of the whole closing day, so a "to" of today shows today's rows.
                predicates.add(builder.lessThan(root.get("createdAt"),
                        to.plusDays(1).atStartOfDay()));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };

        return ActivityLogPageResponse.of(activityLogRepository.findAll(specification, pageable));
    }

    /** The modules the caller has entries for, so the filter offers real values only. */
    @Transactional(readOnly = true)
    public List<String> modules() {
        return activityLogRepository.findDistinctModulesForUser(currentUser.requireUserId());
    }

    /**
     * One entry of the caller's own history.
     *
     * @throws ResponseStatusException 404 when the entry belongs to somebody else, so
     *         the endpoint never confirms that another administrator's entry exists
     */
    @Transactional(readOnly = true)
    public ActivityLog find(Long id) {
        Long actorId = currentUser.requireUserId();
        return activityLogRepository.findByIdAndUserId(id, actorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Activity not found."));
    }

    /**
     * The authenticated caller's id, or {@code null}.
     *
     * <p>Used by callers that need to compare the actor with the record they just
     * changed - updating your own profile is not the same activity as updating
     * somebody else's account.
     */
    public Long currentActorId() {
        return currentUser.getUserId();
    }

    private int clampSize(Integer size) {
        if (size == null || size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
