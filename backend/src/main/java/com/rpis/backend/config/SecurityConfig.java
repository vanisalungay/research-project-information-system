package com.rpis.backend.config;

import com.rpis.backend.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenService tokenService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // Allow all CORS preflight and OPTIONS requests globally
                        .requestMatchers(org.springframework.web.cors.CorsUtils::isPreFlightRequest).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public endpoints
                        .requestMatchers(HttpMethod.POST, "/api/users/login", "/api/users/google-login", "/api/users/seed").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll() // User Sign-up/Registration is public
                        .requestMatchers(HttpMethod.GET, "/api/health").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Uploaded document downloads (Special Orders, attachments) require
                        // authentication; per-proposal ownership is enforced in FileController.
                        .requestMatchers(HttpMethod.GET, "/api/files/**").authenticated()

                        // Administrative endpoints (RPS_ADMIN & RPS_STAFF)
                        .requestMatchers("/api/users/pending", "/api/users/*/approve", "/api/users/*/reject", "/api/users/admin/create").hasAnyRole("RPS_ADMIN", "RPS_STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/users").hasAnyRole("RPS_ADMIN", "RPS_STAFF")

                        // Electronic signature (Manage Profile): the authenticated user's own
                        // signature only. The account is taken from the bearer token, so the
                        // route never accepts a user id and cannot be used to reach someone
                        // else's signature. Declared explicitly so the rule cannot be lost if
                        // the catch-all below ever changes.
                        .requestMatchers("/api/users/me/signature").authenticated()

                        // Proposal Announcement gatekeeper endpoint: any authenticated user
                        // (proponents pre-check before opening the New Proposal form)
                        .requestMatchers(HttpMethod.GET, "/api/proposal-announcements/active").authenticated()

                        // Calls for Proposal a proponent can respond to (the first step of the
                        // Create Proposal workflow): any authenticated user. The service only
                        // returns calls that are open for submissions, so no draft, scheduled,
                        // closed or past-deadline call is ever exposed here.
                        .requestMatchers(HttpMethod.GET, "/api/proposal-announcements/open").authenticated()

                        // Calls for Proposal shown on the same step: the open ones and the
                        // published ones that open later (scheduled). Only ACTIVE calls are
                        // returned - drafts, closed and expired calls stay hidden - so a
                        // proponent can never read an unpublished call from here.
                        .requestMatchers(HttpMethod.GET, "/api/proposal-announcements/available").authenticated()

                        // Cooperating Agency directory (section 02 of the Create Proposal
                        // form): any authenticated user may read it, but only an
                        // administrator (the RPS roles that manage the master data) may
                        // add an agency. The proponent form is read-only over the
                        // directory, so the global list cannot be changed from it.
                        .requestMatchers(HttpMethod.GET, "/api/cooperating-agencies").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/cooperating-agencies")
                        .hasAnyRole("RPS_ADMIN", "RPS_STAFF")

                        // Proponent-facing Call for Proposals details, opened from an
                        // announcement notification: read-only and proponents only. Draft
                        // announcements remain hidden (the service answers 404 for them).
                        .requestMatchers(HttpMethod.GET, "/api/proposal-announcements/*/details")
                            .hasRole("PROPONENT")

                        // Priority Agenda master data (section 05 of the Create Proposal
                        // form): the two read endpoints are available to any authenticated
                        // caller (the Proponent form and the historical labels of a saved
                        // proposal), while every management operation is administrator-only.
                        // The specific GETs must precede the catch-all below, exactly like
                        // the announcement gatekeeper rule above.
                        .requestMatchers(HttpMethod.GET,
                                "/api/priority-agendas/active", "/api/priority-agendas/lookup")
                            .authenticated()
                        .requestMatchers("/api/priority-agendas/**")
                            .hasAnyRole("RPS_ADMIN", "RPS_STAFF")

                        // Proposal Announcement management (RPS_ADMIN & RPS_STAFF only)
                        .requestMatchers("/api/proposal-announcements/**").hasAnyRole("RPS_ADMIN", "RPS_STAFF")

                        // Return-for-Revision: authorized reviewing offices return to RPS first;
                        // only RPS can forward the revision request and set the deadline.
                        .requestMatchers(HttpMethod.PUT, "/api/proposals/*/return-revision")
                            .hasAnyRole("REC", "OVCRIGE", "RPS_ADMIN", "RPS_STAFF")
                        .requestMatchers(HttpMethod.PUT, "/api/proposals/*/forward-revision")
                            .hasAnyRole("RPS_ADMIN", "RPS_STAFF")

                        // Make a Copy of a rejected proposal: proponent-only. The service
                        // additionally enforces that the caller owns the source proposal.
                        .requestMatchers(HttpMethod.POST, "/api/proposals/*/duplicate").hasRole("PROPONENT")

                        // Technology Map image upload (section 13 of the Create Proposal
                        // form): proponent-only. The service additionally enforces that the
                        // caller owns the proposal the image is attached to.
                        .requestMatchers(HttpMethod.POST, "/api/proposals/*/technology-map").hasRole("PROPONENT")

                        // Line-Item Budget file upload (section 25 of the Create Proposal
                        // form): proponent-only. The service additionally enforces that the
                        // caller owns the proposal the file is attached to.
                        .requestMatchers(HttpMethod.POST, "/api/proposals/*/budget-file").hasRole("PROPONENT")

                        // Field-level revision remarks: reviewer roles only. A Proponent must
                        // never be able to write review feedback on their own proposal.
                        .requestMatchers(HttpMethod.PUT, "/api/proposals/*/field-remarks")
                            .hasAnyRole("RPS_ADMIN", "RPS_STAFF", "OVCRIGE", "OVCAF", "REC", "OC")
                        .requestMatchers(HttpMethod.DELETE, "/api/proposals/*/field-remarks/*")
                            .hasAnyRole("RPS_ADMIN", "RPS_STAFF", "OVCRIGE", "OVCAF", "REC", "OC")

                        // Quarterly Progress Reports (implementation-phase reporting)
                        .requestMatchers(HttpMethod.POST, "/api/quarterly-reports", "/api/quarterly-reports/**").hasRole("PROPONENT")
                        .requestMatchers(HttpMethod.PUT, "/api/quarterly-reports/*").hasRole("PROPONENT")
                        .requestMatchers(HttpMethod.GET, "/api/quarterly-reports/**").hasAnyRole("PROPONENT", "RPS_ADMIN", "RPS_STAFF", "OVCRIGE")

                        // All other API requests require authentication
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new TokenAuthenticationFilter(tokenService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
