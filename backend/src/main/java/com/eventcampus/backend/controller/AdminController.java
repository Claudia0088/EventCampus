
package com.eventcampus.backend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.eventcampus.backend.dto.AdminUserResponse;
import com.eventcampus.backend.dto.RoleUpdateRequest;
import com.eventcampus.backend.service.RoleManagementService;
import com.eventcampus.backend.service.RoleManagementService.UserNotFoundException;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final RoleManagementService roleManagementService;

    public AdminController(RoleManagementService roleManagementService) {
        this.roleManagementService = roleManagementService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> listUsers() {
        return ResponseEntity.ok(roleManagementService.listUsers());
    }

    @GetMapping("/roles")
    public ResponseEntity<List<String>> listRoles() {
        return ResponseEntity.ok(roleManagementService.listRoles());
    }

    // Modifier le rôle : compatible avec PUT et PATCH
    @RequestMapping(
        value = "/users/{id}/role",
        method = {RequestMethod.PUT, RequestMethod.PATCH}
    )
    public ResponseEntity<?> changeRole(
            @PathVariable Integer id,
            @RequestBody RoleUpdateRequest request,
            Authentication authentication) {

        String requestedRole = request.getEffectiveRole();

        if (requestedRole == null || requestedRole.isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "Le rôle est obligatoire.")
            );
        }

        try {
            AdminUserResponse updated =
                roleManagementService.changeUserRole(
                    id,
                    requestedRole,
                    authentication.getName()
                );

            return ResponseEntity.ok(Map.of(
                "message", "Rôle mis à jour.",
                "user", updated
            ));

        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("message", e.getMessage())
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                Map.of("message", e.getMessage())
            );

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of("message", e.getMessage())
            );
        }
    }

    // Activer ou désactiver un compte utilisateur
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<?> changeStatus(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        String requestedStatus = request.get("status");

        if (requestedStatus == null || requestedStatus.isBlank()) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "Le statut est obligatoire.")
            );
        }

        try {
            AdminUserResponse updated =
                roleManagementService.changeUserStatus(
                    id,
                    requestedStatus,
                    authentication.getName()
                );

            return ResponseEntity.ok(Map.of(
                "message", "Statut du compte mis à jour.",
                "user", updated
            ));

        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                Map.of("message", e.getMessage())
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                Map.of("message", e.getMessage())
            );

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                Map.of("message", e.getMessage())
            );
        }
    }


@GetMapping("/dashboard")
public ResponseEntity<Map<String, Object>> getDashboard() {

    List<AdminUserResponse> users =
            roleManagementService.listUsers();

    long userAccounts = users.stream()
            .filter(user -> "USER".equalsIgnoreCase(user.getRole()))
            .count();

    long moderatorAccounts = users.stream()
            .filter(user -> "MODERATOR".equalsIgnoreCase(user.getRole()))
            .count();

    long adminAccounts = users.stream()
            .filter(user -> "ADMIN".equalsIgnoreCase(user.getRole()))
            .count();

    Map<String, Object> stats = Map.of(
            "users", users.size(),
            "categories", 0,
            "events", 0,
            "totalUsers", users.size(),
            "userAccounts", userAccounts,
            "moderatorAccounts", moderatorAccounts,
            "adminAccounts", adminAccounts,
            "totalEvents", 0,
            "publishedEvents", 0,
            "pendingEvents", 0
    );

    Map<String, Object> dashboard = Map.of(
            "stats", stats,
            "users", users,
            "categories", List.of(),
            "events", List.of()
    );

    return ResponseEntity.ok(dashboard);
}
}
