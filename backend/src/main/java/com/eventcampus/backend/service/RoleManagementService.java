
package com.eventcampus.backend.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.eventcampus.backend.dto.AdminUserResponse;
import com.eventcampus.backend.entity.Role;
import com.eventcampus.backend.entity.User;
import com.eventcampus.backend.repository.RoleRepository;
import com.eventcampus.backend.repository.UserRepository;

@Service
public class RoleManagementService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SessionRegistry sessionRegistry;

    public RoleManagementService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            SessionRegistry sessionRegistry) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.sessionRegistry = sessionRegistry;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listRoles() {

        return roleRepository.findAll()
                .stream()
                .map(Role::getName)
                .sorted()
                .toList();
    }

    @Transactional
    public AdminUserResponse changeUserRole(
            Integer userId,
            String requestedRole,
            String administratorEmail) {

        if (requestedRole == null || requestedRole.isBlank()) {
            throw new IllegalArgumentException(
                "Le rôle est obligatoire."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                    new UserNotFoundException(
                        "Utilisateur introuvable."
                    )
                );

        if (user.getEmail().equalsIgnoreCase(administratorEmail)) {
            throw new IllegalArgumentException(
                "Vous ne pouvez pas modifier votre propre rôle."
            );
        }

        String roleName = requestedRole
                .trim()
                .toUpperCase(Locale.ROOT);

        Role newRole = roleRepository.findByName(roleName)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Rôle inconnu : " + roleName
                    )
                );

        String currentRole = user.getRole()
                .getName()
                .toUpperCase(Locale.ROOT);

        if ("ADMIN".equals(currentRole)
                && !"ADMIN".equals(roleName)
                && userRepository.countByRole_Name("ADMIN") <= 1) {

            throw new IllegalStateException(
                "Impossible de retirer le rôle du dernier administrateur."
            );
        }

        user.setRole(newRole);

        User savedUser = userRepository.save(user);

        // Expiration des anciennes sessions après changement de rôle
        List<SessionInformation> sessions =
                sessionRegistry.getAllSessions(
                    savedUser.getEmail(),
                    false
                );

        sessions.forEach(SessionInformation::expireNow);

        return toResponse(savedUser);
    }

    private AdminUserResponse toResponse(User user) {

        return new AdminUserResponse(
            user.getIdUser(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.getStatus(),
            user.getRole() == null
                ? null
                : user.getRole().getName(),
            user.getCreatedAt()
        );
    }

    public static class UserNotFoundException
            extends RuntimeException {

        public UserNotFoundException(String message) {
            super(message);
        }
    }
}
