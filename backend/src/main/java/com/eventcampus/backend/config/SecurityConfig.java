
package com.eventcampus.backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.DispatcherType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            com.eventcampus.backend.service.AccountSessionService accounts)
            throws Exception {

        http
            .addFilterBefore(new AccountSessionFilter(accounts), org.springframework.security.web.access.intercept.AuthorizationFilter.class)
            .cors(Customizer.withDefaults())

            // Configuration temporaire pour les tests locaux.
            // À revoir avant tout déploiement en production.
            .csrf(csrf -> csrf.disable())

            .securityContext(context -> context
                .securityContextRepository(securityContextRepository)
            )

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )

            .authorizeHttpRequests(auth -> auth
                // Allow only the container's error dispatch, never a direct public API call.
                .requestMatchers(request -> request.getDispatcherType() == DispatcherType.ERROR
                        && "/error".equals(request.getServletPath())).permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/auth/login",
                    "/api/auth/register"
                ).permitAll()
                .requestMatchers(
                    "/api/auth/me",
                    "/api/auth/logout"
                ).authenticated()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/moderator/**").hasRole("MODERATOR")
                .requestMatchers("/api/user/**").hasRole("USER")
                .requestMatchers("/api/check-in/**").hasAnyRole("ADMIN", "MODERATOR")
                .requestMatchers(HttpMethod.GET, "/api/events", "/api/events/availability")
                    .permitAll()
                .anyRequest().denyAll()
            )

            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) -> {
                    String path = request.getServletPath();
                    if (path.isEmpty()) path = request.getRequestURI().substring(request.getContextPath().length());
                    if (!path.startsWith("/api/check-in/") && !path.matches("/api/user/registrations/[^/]+/qr-code")
                            && !path.matches("/api/user/events/[^/]+/reports") && !path.startsWith("/api/moderator/reports")) {
                        response.sendError(403);
                        return;
                    }
                    response.setStatus(401);
                    response.setContentType("application/json;charset=UTF-8");
                    response.setHeader("Cache-Control", "no-store");
                    response.getWriter().write("{\"message\":\"Session absente ou expirée. Reconnectez-vous.\"}");
                })
                .accessDeniedHandler((request, response, exception) -> {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.setHeader("Cache-Control", "no-store");
                    response.getWriter().write("{\"message\":\"Votre rôle ne permet pas cette action.\"}");
                }))

            .logout(logout -> logout.disable());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${eventcampus.frontend-origins:}") String frontendOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();

        // Autorise les serveurs locaux, y compris ceux utilisant un port.
        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost",
            "http://localhost:*",
            "http://127.0.0.1",
            "http://127.0.0.1:*"
        ));
        // Explicit deployment origins only; no unrestricted credentialed CORS.
        for (String origin : frontendOrigins.split(",")) {
            if (!origin.isBlank()) {
                var uri = java.net.URI.create(origin.trim());
                if (!"https".equals(uri.getScheme()) || uri.getHost() == null
                        || uri.getRawQuery() != null || uri.getRawFragment() != null
                        || uri.getUserInfo() != null || uri.getPort() == 0
                        || (uri.getPath() != null && !uri.getPath().isEmpty())) {
                    throw new IllegalArgumentException("Origine frontend HTTPS invalide.");
                }
                configuration.addAllowedOrigin(uri.toString());
            }
        }

        configuration.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of(
            "Content-Type",
            "Accept",
            "X-EventCampus-Scan",
            "X-EventCampus-Report"
        ));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
