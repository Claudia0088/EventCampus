package com.eventcampus.backend.controller;

import com.eventcampus.backend.dto.LoginRequest;
import com.eventcampus.backend.dto.RegisterRequest;
import com.eventcampus.backend.entity.User;
import com.eventcampus.backend.service.AuthService;
import com.eventcampus.backend.service.RegisterService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger =
            LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final RegisterService registerService;
    private final SecurityContextRepository securityContextRepository;
    private final SessionRegistry sessionRegistry;

    public AuthController(
            AuthService authService,
            RegisterService registerService,
            SecurityContextRepository securityContextRepository,
            SessionRegistry sessionRegistry) {
        this.authService = authService;
        this.registerService = registerService;
        this.securityContextRepository = securityContextRepository;
        this.sessionRegistry = sessionRegistry;
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
            sessionRegistry.removeSessionInformation(oldSession.getId());
            oldSession.invalidate();
        }

        HttpSession newSession = httpRequest.getSession(true);

        sessionRegistry.registerNewSession(
                newSession.getId(),
                user.getEmail()
        );

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
            @Valid @RequestBody RegisterRequest request,
            BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Les données d'inscription sont invalides.")
            );
        }

        try {
            User user = registerService.register(request);

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "message", "Inscription réussie",
                            "email", user.getEmail()
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of("message", "Cette adresse e-mail est déjà utilisée.")
            );

        } catch (IllegalStateException | DataAccessException e) {
            logger.error("Erreur interne pendant l'inscription.", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("message", "Impossible de créer le compte pour le moment.")
            );
        }
    }

}