
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

    // Connexion d'un utilisateur
    @Transactional
    public User login(String email, String password) {

        if (email == null || email.isBlank()
                || password == null || password.isBlank()) {
            return null;
        }

        User user = userRepository
                .findByEmail(email.trim().toLowerCase())
                .orElse(null);

        if (user == null || user.getPassword() == null) {
            return null;
        }

        String storedPassword = user.getPassword();

        boolean passwordMatches;

        // Vérification des mots de passe déjà chiffrés avec BCrypt
        if (storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$")) {

            passwordMatches = passwordEncoder.matches(
                    password,
                    storedPassword
            );

        } else {
            // Compatibilité temporaire avec les anciens mots de passe
            // enregistrés en clair, puis migration vers BCrypt.
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

    // Inscription d'un nouvel utilisateur
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
                    + "Le mot de passe doit contenir "
                    + "au moins 8 caractères."
            );
        }

        String normalizedEmail = email.trim().toLowerCase();

        // Vérifier si l'adresse e-mail existe déjà
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException(
                    "Cette adresse e-mail est déjà utilisée."
            );
        }

        // Récupérer le rôle USER dans la base de données
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException(
                        "Le rôle USER n'existe pas dans la base de données."
                ));

        User user = new User();

        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(normalizedEmail);

        // Ne jamais enregistrer le mot de passe en clair
        user.setPassword(passwordEncoder.encode(password));

        user.setStatus("ACTIVE");
        user.setCreatedAt(LocalDateTime.now());
        user.setRole(userRole);

        return userRepository.save(user);
    }

    // Rechercher un utilisateur par son adresse e-mail
    @Transactional(readOnly = true)
    public User findByEmail(String email) {

        if (email == null || email.isBlank()) {
            return null;
        }

        return userRepository
                .findByEmail(email.trim().toLowerCase())
                .orElse(null);
    }
}
