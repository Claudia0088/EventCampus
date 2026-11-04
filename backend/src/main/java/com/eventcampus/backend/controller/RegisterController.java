package com.eventcampus.backend.controller;

import com.eventcampus.backend.dto.RegisterRequest;
import com.eventcampus.backend.entity.User;
import com.eventcampus.backend.service.RegisterService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/register")
public class RegisterController {

    private final RegisterService registerService;

    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
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
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("message", "Impossible de créer le compte pour le moment.")
            );
        }
    }
}
