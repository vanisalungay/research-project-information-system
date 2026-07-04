package com.rpis.backend.config;

import com.rpis.backend.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@RequiredArgsConstructor
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        System.out.println("========== TokenAuthenticationFilter ==========");
        System.out.println("Request URI: " + request.getRequestURI());
        System.out.println("Auth Header: " + authHeader);
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            System.out.println("Extracted Token: " + token);
            boolean isValid = tokenService.validateToken(token);
            System.out.println("Token Valid: " + isValid);
            
            if (isValid) {
                Long userId = tokenService.getUserIdFromToken(token);
                String role = tokenService.getRoleFromToken(token);
                System.out.println("Parsed UserId: " + userId + ", Role: " + role);

                if (userId != null && role != null) {
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userId, // Principal
                            null,   // Credentials
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())) // Authorities
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    System.out.println("Authentication successfully set in SecurityContext.");
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
