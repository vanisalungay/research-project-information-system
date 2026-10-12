package com.rpis.backend.security;

import com.rpis.backend.model.Proposal;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

/**
 * Resolves the identity of the caller from the Spring Security context.
 * <p>
 * {@code TokenAuthenticationFilter} stores the authenticated user's database id as
 * the principal and grants a single {@code ROLE_<role>} authority. Authorization
 * decisions must always be derived from this server-side identity, never from a
 * user id supplied by the frontend.
 */
@Component
public class CurrentUser {

    /** The only role that is scoped to its own records. */
    private static final String PROPONENT = "PROPONENT";

    /**
     * Roles explicitly authorized to see the university-wide proposal set. Any role
     * that is not on this allow-list (including an unrecognized/misdetected role) is
     * treated as own-records-only, so the endpoint fails closed rather than leaking
     * every proposal.
     */
    private static final Set<String> PRIVILEGED_ROLES = Set.of(
            "RPS_ADMIN", "RPS_STAFF", "OVCRIGE", "OVCAF", "REC", "OC");

    /** Returns the authenticated user's id, or {@code null} when unauthenticated. */
    public Long getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        if (principal instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /** Returns the authenticated user's id or fails with 401. */
    public Long requireUserId() {
        Long userId = getUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }
        return userId;
    }

    /** Returns the caller's role (without the {@code ROLE_} prefix), or {@code null}. */
    public String getRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String value = authority.getAuthority();
            if (value != null && value.startsWith("ROLE_")) {
                return value.substring("ROLE_".length());
            }
        }
        return null;
    }

    /** True when the caller is a Faculty Proponent (own-records-only access). */
    public boolean isProponent() {
        String role = getRole();
        return role != null && PROPONENT.equalsIgnoreCase(role);
    }

    /**
     * True only for the explicitly authorized staff/reviewer roles that may see the
     * university-wide proposal set. Everything else is scoped to its own records.
     */
    public boolean isPrivileged() {
        String role = getRole();
        return role != null && PRIVILEGED_ROLES.contains(role.toUpperCase());
    }

    /**
     * Guards access to a single proposal. Proponents may only reach their own
     * proposals; all other (authorized) roles retain the existing broad access
     * required by the review/approval workflow.
     */
    public void requireAccessTo(Proposal proposal) {
        if (!isProponent()) {
            return;
        }
        Long userId = requireUserId();
        Long ownerId = proposal != null && proposal.getProponent() != null
                ? proposal.getProponent().getId()
                : null;
        if (ownerId == null || !ownerId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorized to access this proposal.");
        }
    }

    /** Fails with 403 when the given user id is not the caller's own id. */
    public void requireSelf(Long userId) {
        if (userId == null || !userId.equals(requireUserId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not authorized to access this resource.");
        }
    }
}
