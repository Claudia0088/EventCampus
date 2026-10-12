
package com.eventcampus.backend.service;

import com.eventcampus.backend.entity.Role;
import com.eventcampus.backend.entity.User;
import com.eventcampus.backend.repository.RoleRepository;
import com.eventcampus.backend.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            RoleRepository roleRepository) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Connecte un utilisateur.
     * Les anciens mots de passe en clair sont convertis
     * en BCrypt après une connexion réussie.
     */
    @Transactional
    public User login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null || password == null) {
            return null;
        }

        String storedPassword = user.getPassword();

        if (storedPassword == null) {
            return null;
        }

        boolean passwordMatches;

        if (storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {

            passwordMatches =
                    passwordEncoder.matches(password, storedPassword);

        } else {
            // Compatibilité temporaire avec les anciens comptes.
            passwordMatches = storedPassword.equals(password);

            if (passwordMatches) {
                user.setPassword(passwordEncoder.encode(password));
                userRepository.save(user);
            }
        }

        if (!passwordMatches) {
            return null;
        }

        return user;
    }

    /**
     * Inscrit un nouvel utilisateur avec un mot de passe haché.
     */
    @Transactional
    public User register(
            String firstName,
            String lastName,
            String email,
            String password) {

        if (firstName == null || firstName.isBlank()
                || lastName == null || lastName.isBlank()
                || email == null || email.isBlank()
                || password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Les informations sont invalides. "
                    + "Le mot de passe doit contenir au moins 8 caractères.");
        }

        String normalizedEmail = email.trim().toLowerCase();

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException(
                    "Cette adresse e-mail est déjà utilisée.");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException(
                        "Le rôle USER n'existe pas dans la base de données."));

        User user = new User();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(password));
        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(userRole);

        return userRepository.save(user);
    }
}
