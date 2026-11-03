
package com.eventcampus.backend.service;

import com.eventcampus.backend.entity.User;
import com.eventcampus.backend.repository.UserRepository;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository) {

        this.userRepository = userRepository;
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
