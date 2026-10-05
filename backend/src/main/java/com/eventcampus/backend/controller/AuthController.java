package com.eventcampus.backend.controller;

import com.eventcampus.backend.dto.LoginRequest;
import com.eventcampus.backend.entity.User;
import com.eventcampus.backend.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request) {

        User user = authService.login(
                request.getEmail(),
                request.getPassword()
        );

        if (user == null) {
            return "Email ou mot de passe incorrect";
        }

        return "Connexion réussie";
    }
}