package com.eventcampus.backend.controller;
import com.eventcampus.backend.service.RegistrationService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.*;

@RestController
public class RegistrationController {
    private final RegistrationService registrations;
    public RegistrationController(RegistrationService registrations) { this.registrations = registrations; }
    @GetMapping("/api/events/availability") public List<RegistrationService.Availability> availability() { return registrations.availability(); }
    @GetMapping("/api/user/registrations/mine") public List<RegistrationService.Item> mine(Principal principal) { return registrations.mine(principal.getName()); }
    @PostMapping("/api/user/events/{id}/registrations") public ResponseEntity<?> register(Principal principal, @PathVariable Integer id) {
        registrations.register(principal.getName(), id); return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Inscription confirmée."));
    }
    @PatchMapping("/api/user/registrations/{id}/cancel") public Map<String,String> cancel(Principal principal, @PathVariable Integer id) {
        registrations.cancel(principal.getName(), id); return Map.of("message", "Inscription annulée. Votre place a été libérée.");
    }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> error(ResponseStatusException error) { return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason())); }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class) public ResponseEntity<?> duplicate(Exception error) { return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "Cette inscription existe déjà. Actualisez la liste.")); }
    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class) public ResponseEntity<?> invalid(Exception error) { return ResponseEntity.badRequest().body(Map.of("message", "Identifiant invalide.")); }
}
