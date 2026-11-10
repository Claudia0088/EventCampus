
package com.eventcampus.backend.config;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private static final Set<String> ALLOWED_ORIGINS = Set.of(
        "http://localhost:5500",
        "http://127.0.0.1:5500",
        "http://localhost:5501",
        "http://127.0.0.1:5501"
    );

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.copyOf(ALLOWED_ORIGINS));

        config.setAllowedMethods(List.of(
            "GET", "POST", "PUT", "PATCH",
            "DELETE", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
            "Content-Type", "Accept", "X-Requested-With"
        ));

        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
            new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository,
            SessionRegistry sessionRegistry) throws Exception {

        http
            .cors(Customizer.withDefaults())

            // Vérification de l'origine des requêtes sensibles
            .addFilterBefore(
                new OriginValidationFilter(),
                UsernamePasswordAuthenticationFilter.class
            )

            .csrf(csrf -> csrf.disable())

            .securityContext(context ->
                context.securityContextRepository(
                    securityContextRepository
                )
            )

            .sessionManagement(session ->
                session.maximumSessions(-1)
                    .sessionRegistry(sessionRegistry)
            )

            .authorizeHttpRequests(auth -> auth

                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register"
                ).permitAll()

                .requestMatchers("/api/register/**").permitAll()

                .anyRequest().authenticated()
            );

        return http.build();
    }

    /**
     * Vérifie l'origine des requêtes qui modifient les données.
     */
    private static class OriginValidationFilter
            extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain)
                throws ServletException, IOException {

            String method = request.getMethod();

            boolean safeMethod =
                HttpMethod.GET.matches(method)
                || HttpMethod.HEAD.matches(method)
                || HttpMethod.OPTIONS.matches(method);

            if (safeMethod) {
                filterChain.doFilter(request, response);
                return;
            }

            String origin = request.getHeader("Origin");

            if (origin == null || !ALLOWED_ORIGINS.contains(origin)) {
                response.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Origine de la requête non autorisée."
                );
                return;
            }

            filterChain.doFilter(request, response);
        }
    }
}
