package com.eventcampus.backend.service;

import com.eventcampus.backend.entity.User;
import com.eventcampus.backend.entity.Role;
import com.eventcampus.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;    

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
// --- Branche feature/api-login (Votre partie) ---
    public User login(LoginRequest request) {

        return userRepository.findByEmail(request.getEmail())
               .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect");
        }
        return user;
    }
}