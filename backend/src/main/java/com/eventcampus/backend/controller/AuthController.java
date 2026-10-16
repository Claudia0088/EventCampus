package com.eventcampus.backend.controller;

import com.eventcampus.backend.dto.LoginRequest;
import com.eventcampus.backend.entity.User;
import com.eventcampus.backend.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            AuthService authService,
            SecurityContextRepository securityContextRepository) {
        this.authService = authService;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {

        if (request == null
                || request.getEmail() == null
                || request.getPassword() == null
                || request.getEmail().isBlank()
                || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "L'e-mail et le mot de passe sont obligatoires.")
            );
        }

        User user = authService.login(
                request.getEmail(),
                request.getPassword()
        );

        if (user == null
                || user.getRole() == null
                || user.getRole().getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "E-mail ou mot de passe incorrect.")
            );
        }

        String roleName = user.getRole().getName().toUpperCase();

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + roleName))
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        HttpSession oldSession = httpRequest.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }

        httpRequest.getSession(true);

        securityContextRepository.saveContext(
                context, httpRequest, response
        );

        return ResponseEntity.ok(Map.of(
                "message", "Connexion réussie",
                "email", user.getEmail(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "role", roleName
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentUser(Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof String email)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "Vous n'êtes pas connecté.")
            );
        }

        User user = authService.findByEmail(email);

        if (user == null || user.getRole() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    Map.of("message", "Utilisateur introuvable.")
            );
        }

        return ResponseEntity.ok(Map.of(
                "email", user.getEmail(),
                "firstName", user.getFirstName(),
                "lastName", user.getLastName(),
                "role", user.getRole().getName()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {

        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok(
                Map.of("message", "Déconnexion réussie")
        );
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody RegisterRequest request) {

        if (request == null
                || request.getFirstName() == null
                || request.getLastName() == null
                || request.getEmail() == null
                || request.getPassword() == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Tous les champs sont obligatoires.")
            );
        }

        try {
            User user = authService.register(
                    request.getFirstName(),
                    request.getLastName(),
                    request.getEmail(),
                    request.getPassword()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "message", "Inscription réussie",
                            "email", user.getEmail()
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", e.getMessage())
            );

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("message", e.getMessage())
            );
        }
    }

    public static class RegisterRequest {

        private String firstName;
        private String lastName;
        private String email;
        private String password;

        public RegisterRequest() {
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}