package com.rpis.backend.config;

import com.rpis.backend.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * <p>Verification is stateless, so any instance behind the load balancer can
 * authenticate a token issued by any other instance.
 */
@Slf4j
@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            TokenService.ParsedToken parsed = tokenService.parseValid(token);

            if (parsed != null && parsed.userId() != null && parsed.role() != null) {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        parsed.userId(), // Principal
                        null,            // Credentials
                        Collections.singletonList(
                                new SimpleGrantedAuthority("ROLE_" + parsed.role().toUpperCase()))
                );
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (log.isDebugEnabled()) {
                    log.debug("Authenticated request {} {} as user {} with role {}",
                            request.getMethod(), request.getRequestURI(), parsed.userId(), parsed.role());
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}

