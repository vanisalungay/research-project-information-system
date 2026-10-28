package com.rpis.backend.config;

import com.rpis.backend.model.UserRoles;
import com.rpis.backend.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Value("${rpis.cors.allowed-origins:*}") String allowedOrigins) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource(allowedOrigins)))
                .authorizeHttpRequests(auth -> auth
                        // Allow all CORS preflight and OPTIONS requests globally
                        .requestMatchers(org.springframework.web.cors.CorsUtils::isPreFlightRequest).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Public endpoints
                        .requestMatchers(HttpMethod.POST, "/api/users/login", "/api/users/google-login")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/users").permitAll() // User Sign-up/Registration is
                                                                                    // public
                        .requestMatchers(HttpMethod.GET, "/api/health", "/api/health/ready").permitAll()
                        // Actuator liveness only, and with component details suppressed
                        // (management.endpoint.health.show-details=never), so the load
                        // balancer can probe it without authentication. The metrics
                        // endpoint stays behind authentication via the catch-all below.
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Uploaded document downloads (Special Orders, attachments) require
                        // authentication; per-proposal ownership is enforced in FileController.
                        .requestMatchers(HttpMethod.GET, "/api/files/**").authenticated()

                        // Administrative endpoints (RPS_ADMIN & RPS_STAFF)
                        .requestMatchers("/api/users/pending", "/api/users/*/approve", "/api/users/*/reject",
                                "/api/users/admin/create")
                        .hasAnyRole(UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF)
                        .requestMatchers(HttpMethod.GET, "/api/users").hasAnyRole(UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF)

                        // Dean / Department Chair account management: RPS_ADMIN only.
                        // Granting the Dean role is an administrative act (it carries an
                        // endorsement scope), so it is deliberately narrower than the
                        // general staff account routes and the service re-checks the role.
                        .requestMatchers("/api/users/deans", "/api/users/deans/**").hasRole(UserRoles.RPS_ADMIN)

                        // Development seed trigger: RPS_ADMIN only, and DevSeedController
                        // additionally refuses to write unless the target database is the
                        // isolated rpis_test database. The route used to be public.
                        .requestMatchers(HttpMethod.POST, "/api/users/seed").hasRole(UserRoles.RPS_ADMIN)

                        // Dean endorsement workflow. The requesting Proponent raises the
                        // request; only a Dean may approve, reject or reissue it. Every route
                        // is additionally scoped in EndorsementRequestService, so the
                        // frontend never decides who may see or decide what.
                        .requestMatchers(HttpMethod.POST, "/api/endorsements").hasRole(UserRoles.PROPONENT)
                        .requestMatchers(HttpMethod.PUT, "/api/endorsements/*/approve").hasRole(UserRoles.DEAN)
                        .requestMatchers(HttpMethod.PUT, "/api/endorsements/*/reject").hasRole(UserRoles.DEAN)
                        .requestMatchers(HttpMethod.PUT, "/api/endorsements/*/reissue")
                        .hasAnyRole(UserRoles.DEAN, UserRoles.RPS_ADMIN)
                        .requestMatchers("/api/endorsements/**", "/api/endorsements").authenticated()

                        // Certifications: the Proponent's own documents (the service
                        // resolves the owner from the token) and the document download.
                        .requestMatchers("/api/certifications", "/api/certifications/**").authenticated()

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
                        .hasAnyRole(UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF)

                        // Proponent-facing Call for Proposals details, opened from an
                        // announcement notification: read-only and proponents only. Draft
                        // announcements remain hidden (the service answers 404 for them).
                        .requestMatchers(HttpMethod.GET, "/api/proposal-announcements/*/details")
                        .hasRole(UserRoles.PROPONENT)

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
                        .hasAnyRole(UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF)

                        // Proposal Announcement management (RPS_ADMIN & RPS_STAFF only)
                        .requestMatchers("/api/proposal-announcements/**").hasAnyRole(UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF)

                        // Return-for-Revision: authorized reviewing offices return to RPS first;
                        // only RPS can forward the revision request and set the deadline.
                        .requestMatchers(HttpMethod.PUT, "/api/proposals/*/return-revision")
                        .hasAnyRole(UserRoles.REC, UserRoles.OVCRIGE, UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF)
                        .requestMatchers(HttpMethod.PUT, "/api/proposals/*/forward-revision")
                        .hasAnyRole(UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF)

                        // Make a Copy of a rejected proposal: proponent-only. The service
                        // additionally enforces that the caller owns the source proposal.
                        .requestMatchers(HttpMethod.POST, "/api/proposals/*/duplicate").hasRole(UserRoles.PROPONENT)

                        // Technology Map image upload (section 13 of the Create Proposal
                        // form): proponent-only. The service additionally enforces that the
                        // caller owns the proposal the image is attached to.
                        .requestMatchers(HttpMethod.POST, "/api/proposals/*/technology-map").hasRole(UserRoles.PROPONENT)

                        // Line-Item Budget file upload (section 25 of the Create Proposal
                        // form): proponent-only. The service additionally enforces that the
                        // caller owns the proposal the file is attached to.
                        .requestMatchers(HttpMethod.POST, "/api/proposals/*/budget-file").hasRole(UserRoles.PROPONENT)

                        // Field-level revision remarks: reviewer roles only. A Proponent must
                        // never be able to write review feedback on their own proposal.
                        .requestMatchers(HttpMethod.PUT, "/api/proposals/*/field-remarks")
                        .hasAnyRole(UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF, UserRoles.OVCRIGE, UserRoles.OVCAF, UserRoles.REC, UserRoles.OC)
                        .requestMatchers(HttpMethod.DELETE, "/api/proposals/*/field-remarks/*")
                        .hasAnyRole(UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF, UserRoles.OVCRIGE, UserRoles.OVCAF, UserRoles.REC, UserRoles.OC)

                        // Quarterly Progress Reports (implementation-phase reporting)
                        .requestMatchers(HttpMethod.POST, "/api/quarterly-reports", "/api/quarterly-reports/**")
                        .hasRole(UserRoles.PROPONENT)
                        .requestMatchers(HttpMethod.PUT, "/api/quarterly-reports/*").hasRole(UserRoles.PROPONENT)
                        .requestMatchers(HttpMethod.GET, "/api/quarterly-reports/**")
                        .hasAnyRole(UserRoles.PROPONENT, UserRoles.RPS_ADMIN, UserRoles.RPS_STAFF, UserRoles.OVCRIGE)

                        // All other API requests require authentication
                        .anyRequest().authenticated())
                .addFilterBefore(new TokenAuthenticationFilter(tokenService),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * The password encoder every account password goes through.
     *
     * <p>BCrypt, because it is already on the classpath ({@code spring-security-crypto})
     * and needs no per-deployment tuning. Argon2 would need the extra
     * {@code bouncycastle} dependency and host-specific memory parameters; it is a
     * drop-in swap here (return an {@code Argon2PasswordEncoder}) if that becomes the
     * requirement.
     *
     * <p>Strength 10 is the Spring default and costs roughly 50-100 ms of CPU per
     * verification. That cost is deliberate - it is what makes an offline attack on a
     * stolen hash expensive - and it is measured against the login-burst load test
     * rather than assumed. Nothing weaker is used: a password column that can be read
     * by anyone with database access is the vulnerability this closes.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS policy for the API.
     *
     * <p>The allowed origins are read from {@code rpis.cors.allowed-origins} (a
     * comma-separated list, {@code *} meaning "any") instead of being hard-coded, so
     * a deployment behind a load balancer can restrict the API to its own front-end
     * origin without a code change. The default keeps the previous permissive
     * behaviour, so nothing that worked before stops working.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${rpis.cors.allowed-origins:*}") String allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        if (origins.isEmpty() || origins.contains("*")) {
            // allowedOriginPatterns (not allowedOrigins) so a wildcard stays usable
            // together with credentials, which is what the bearer-token setup needs.
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            configuration.setAllowedOrigins(origins);
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // Bearer tokens are sent in the Authorization header, not as a cookie, so no
        // credentials exchange is required. Keeping this false is deliberate: it is
        // what makes a wildcard origin safe.
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
