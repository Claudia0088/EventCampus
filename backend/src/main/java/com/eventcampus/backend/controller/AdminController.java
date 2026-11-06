
package com.eventcampus.backend.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
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

    public AdminController(
            RoleManagementService roleManagementService) {
        this.roleManagementService = roleManagementService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> listUsers() {
        return ResponseEntity.ok(
            roleManagementService.listUsers()
        );
    }

    @GetMapping("/roles")
    public ResponseEntity<List<String>> listRoles() {
        return ResponseEntity.ok(
            roleManagementService.listRoles()
        );
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> changeRole(
            @PathVariable Integer id,
            @Valid @RequestBody RoleUpdateRequest request,
            BindingResult bindingResult,
            Authentication authentication) {

        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(
                Map.of("message", "Le nom du rôle est obligatoire.")
            );
        }

        try {
            AdminUserResponse updated =
                roleManagementService.changeUserRole(
                    id,
                    request.getRoleName(),
                    authentication.getName()
                );

            return ResponseEntity.ok(Map.of(
                "message", "Rôle mis à jour. L'utilisateur devra se reconnecter.",
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
}
