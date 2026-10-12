package com.rpis.backend.security;

import com.rpis.backend.model.Proposal;
import com.rpis.backend.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies how the authenticated identity (as established by
 * {@code TokenAuthenticationFilter}) is resolved and used for proposal ownership.
 */
class CurrentUserTest {

    private final CurrentUser currentUser = new CurrentUser();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Long userId, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private Proposal proposalOwnedBy(Long ownerId) {
        User owner = new User();
        owner.setId(ownerId);
        Proposal proposal = new Proposal();
        proposal.setId(100L);
        proposal.setProponent(owner);
        return proposal;
    }

    @Test
    void resolvesUserIdAndRoleFromTheSecurityContext() {
        authenticate(7L, "PROPONENT");
        assertEquals(7L, currentUser.getUserId());
        assertEquals("PROPONENT", currentUser.getRole());
        assertTrue(currentUser.isProponent());
    }

    @Test
    void proponentCanAccessOwnProposal() {
        authenticate(7L, "PROPONENT");
        currentUser.requireAccessTo(proposalOwnedBy(7L));
    }

    @Test
    void proponentCannotAccessAnotherUsersProposal() {
        authenticate(7L, "PROPONENT");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> currentUser.requireAccessTo(proposalOwnedBy(8L)));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void proponentCannotAccessAnOwnerlessProposal() {
        authenticate(7L, "PROPONENT");
        Proposal orphan = new Proposal();
        orphan.setId(101L);
        assertThrows(ResponseStatusException.class, () -> currentUser.requireAccessTo(orphan));
    }

    @Test
    void staffRetainBroadProposalAccess() {
        authenticate(2L, "RPS_STAFF");
        assertFalse(currentUser.isProponent());
        // Staff may open any proposal, including one owned by someone else.
        currentUser.requireAccessTo(proposalOwnedBy(99L));
    }

    @Test
    void requireSelfRejectsAnotherUserId() {
        authenticate(7L, "PROPONENT");
        currentUser.requireSelf(7L);
        assertThrows(ResponseStatusException.class, () -> currentUser.requireSelf(8L));
    }

    @Test
    void unauthenticatedCallerHasNoIdentity() {
        assertThrows(ResponseStatusException.class, currentUser::requireUserId);
    }

    @Test
    void knownStaffAndReviewerRolesArePrivileged() {
        for (String role : List.of("RPS_ADMIN", "RPS_STAFF", "OVCRIGE", "OVCAF", "REC", "OC")) {
            authenticate(2L, role);
            assertTrue(currentUser.isPrivileged(), role + " should be privileged");
        }
    }

    @Test
    void proponentAndUnknownRolesAreNotPrivileged() {
        authenticate(7L, "PROPONENT");
        assertFalse(currentUser.isPrivileged());

        authenticate(7L, "SOME_UNKNOWN_ROLE");
        assertFalse(currentUser.isPrivileged(), "an unrecognized role must fail closed");
    }
}
