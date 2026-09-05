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

                        // Administrative endpoints (RPS_ADMIN & RPS_STAFF)
                        .requestMatchers("/api/users/pending", "/api/users/*/approve", "/api/users/*/reject", "/api/users/admin/create").hasAnyRole("RPS_ADMIN", "RPS_STAFF")
                        .requestMatchers(HttpMethod.GET, "/api/users").hasAnyRole("RPS_ADMIN", "RPS_STAFF")

                        // Application Cycle gatekeeper endpoint: any authenticated user
                        // (proponents pre-check before opening the New Proposal form)
                        .requestMatchers(HttpMethod.GET, "/api/application-cycles/active").authenticated()

                        // Application Cycle management (RPS_ADMIN & RPS_STAFF only)
                        .requestMatchers("/api/application-cycles/**").hasAnyRole("RPS_ADMIN", "RPS_STAFF")

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
