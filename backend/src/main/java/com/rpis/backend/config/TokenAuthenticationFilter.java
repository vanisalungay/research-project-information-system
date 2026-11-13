package com.rpis.backend.config;

import com.rpis.backend.model.User;
import com.rpis.backend.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Authenticates a request from its {@code Authorization: Bearer <token>} header.
 *
 * <p>The token is verified once ({@link TokenService#parseValid(String)}) and the
 * result is used for both the user id and the role, so a request decodes its token
 * a single time. The previous implementation decoded the token three times and
 * printed six lines to {@code System.out} on every request; {@code System.out} is
 * a single synchronized stream, so at 500 concurrent users those prints were the
 * largest avoidable cost in the whole request path. They are now a single
 * {@code DEBUG} line, which is off unless a developer turns it on.
 *
 * <p>The token is then checked against the account's current token version, stored
 * in the shared database. Signing out increments that version
 * ({@link com.rpis.backend.service.UserService#revokeTokens(Long)}), so a token that
 * was valid before the sign-out no longer authenticates on any instance. The token
 * itself stays stateless; only the small counter is shared.
 */
@Slf4j
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final com.rpis.backend.repository.UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        User authenticatedUser = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            TokenService.ParsedToken parsed = tokenService.parseValid(token);

            if (parsed != null && parsed.userId() != null && parsed.role() != null) {
                User user = userRepository.findById(parsed.userId()).orElse(null);

                // A token is accepted only for the account generation it was issued for.
                // Signing out increments the stored version, so the token presented on
                // the next request no longer matches and the session is over.
                if (user == null || user.getTokenVersion() != parsed.tokenVersion()) {
                    // A browser may still hold a token that no longer matches (a sign-out
                    // from another tab, or a token revoked on another device). Public
                    // endpoints - sign-in above all - must stay usable so it can recover;
                    // only a protected request is refused.
                    if (!isPublicRequest(request)) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("text/plain");
                        response.getWriter().write(
                                "Your session has ended. Please sign in again.");
                        return;
                    }
                } else {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            parsed.userId(), // Principal
                            null,            // Credentials
                            Collections.singletonList(
                                    new SimpleGrantedAuthority("ROLE_" + parsed.role().toUpperCase()))
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    authenticatedUser = user;

                    if (log.isDebugEnabled()) {
                        log.debug("Authenticated request {} {} as user {} with role {}",
                                request.getMethod(), request.getRequestURI(), parsed.userId(), parsed.role());
                    }
                }
            }
        }

        // An account still holding an administrator-issued temporary password may only reach
        // the password-change endpoint and the authentication endpoints. This is the
        // server-side half of the rule: the client redirect is a convenience, and without this
        // a temporary password could be used against every other API.
        if (authenticatedUser != null
                && Boolean.TRUE.equals(authenticatedUser.getMustChangePassword())
                && !isAllowedWhileChangingPassword(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("text/plain");
            response.getWriter().write(
                    "You must change your temporary password before using the application.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * The endpoints the security configuration serves without authentication. A stale
     * token must not make them unreachable, so they are exempt from the version check.
     */
    private static boolean isPublicRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return false;
        }
        if ("/error".equals(path)) {
            return true;
        }
        if (HttpMethod.POST.matches(request.getMethod())) {
            return "/api/users/login".equals(path)
                    || "/api/users/google-login".equals(path)
                    || "/api/users".equals(path);
        }
        if (HttpMethod.GET.matches(request.getMethod())) {
            return "/api/health".equals(path)
                    || "/api/health/ready".equals(path)
                    || "/actuator/health".equals(path);
        }
        return false;
    }

    /**
     * The only requests such an account may make: the password change itself, the
     * authentication endpoints, the reads that render the change-password page, and
     * signing out. Signing out must always work, even before the mandatory password
     * change, so the account can end its session.
     */
    private static boolean isAllowedWhileChangingPassword(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return false;
        }
        return path.startsWith("/api/users/change-password")
                || path.startsWith("/api/users/login")
                || path.startsWith("/api/users/google-login")
                || path.equals("/api/users/logout")
                || path.startsWith("/api/users/me");
    }
}