package com.rpis.backend.model;

import com.rpis.backend.security.CurrentUser;
import com.rpis.backend.service.DeanAccountService;
import com.rpis.backend.service.EndorsementRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the canonical role vocabulary.
 *
 * <p>These are the assertions that make a rename safe: the role value is stored in
 * {@code users.role}, embedded in the bearer token and compared in the security
 * configuration, so the three layers have to agree. They had not: the endorsement
 * decision routes required {@code DEAN} while every account carried
 * {@code RPS_DEAN}, and every Dean decision answered 403.
 *
 * <p>The Dean is an independent role, not part of RPS, which is why its value carries
 * no {@code RPS_} prefix. It is also deliberately not a privileged role: a Dean must
 * never inherit the university-wide proposal access that the RPS and reviewing roles
 * hold.
 */
class UserRolesTest {

    @Test
    void theDeanIsAnIndependentRoleSpelledDean() {
        assertEquals("DEAN", UserRoles.DEAN, "the Dean role value must be exactly DEAN");
        assertFalse(UserRoles.DEAN.startsWith("RPS_"),
                "the Dean is not an RPS sub-role, so its value must not carry the RPS_ prefix");
        assertFalse(UserRoles.DEAN.contains("_DEAN"),
                "the Dean role must not be spelled as a prefixed variant");
    }

    @Test
    void everyDeanRoleConstantAgreesWithTheCanonicalValue() {
        assertEquals(UserRoles.DEAN, DeanAccountService.ROLE_DEAN,
                "the Dean account service must use the canonical value");
        assertEquals(UserRoles.DEAN, EndorsementRequestService.ROLE_DEAN,
                "the endorsement workflow must use the canonical value");
    }

    @Test
    void theRpsRolesKeepTheirRpsPrefix() {
        assertTrue(UserRoles.RPS_ADMIN.startsWith("RPS_"));
        assertTrue(UserRoles.RPS_STAFF.startsWith("RPS_"));
        assertTrue(UserRoles.PROPONENT.equals("PROPONENT"));
    }

    @Test
    void aDeanHoldsNoPrivilegedAccess() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                1L, null, List.of(new SimpleGrantedAuthority("ROLE_" + UserRoles.DEAN))));
        try {
            CurrentUser currentUser = new CurrentUser();

            assertEquals(UserRoles.DEAN, currentUser.getRole(),
                    "the role must survive the ROLE_ prefix the token filter adds");
            assertFalse(currentUser.isPrivileged(),
                    "a Dean must not see the university-wide proposal set");
            assertFalse(currentUser.isProponent(),
                    "a Dean is not a Proponent, so it must not receive Proponent-only routes");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
