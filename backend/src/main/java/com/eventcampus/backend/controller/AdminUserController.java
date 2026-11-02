package com.eventcampus.backend.controller;

import com.eventcampus.backend.service.AdminUserService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService users;
    public AdminUserController(AdminUserService users) { this.users = users; }
    public record RoleChange(String role) {}
    public record StatusChange(String status) {}
    @PatchMapping("/{id}/role") public Map<String,String> role(Principal actor, @PathVariable Integer id, @RequestBody RoleChange request) {
        users.role(actor.getName(), id, request.role()); return Map.of("message", "Rôle mis à jour. Une nouvelle connexion sera nécessaire pour le compte concerné.");
    }
    @PatchMapping("/{id}/status") public Map<String,String> status(Principal actor, @PathVariable Integer id, @RequestBody StatusChange request) {
        users.status(actor.getName(), id, request.status()); return Map.of("message", "Statut du compte mis à jour.");
    }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> error(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason()));
    }
}
