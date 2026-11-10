
package com.eventcampus.backend.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

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

    private static final Set<String> ALLOWED_ROLES =
            Set.of("USER", "MODERATOR", "ADMIN");

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("ACTIVE", "INACTIVE");

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

        validateAdministrator(administratorEmail);

        String roleName = requestedRole.trim()
                .toUpperCase(Locale.ROOT);

        if (!ALLOWED_ROLES.contains(roleName)) {
            throw new IllegalArgumentException(
                    "Rôle non autorisé. Valeurs possibles : USER, MODERATOR, ADMIN."
            );
        }

        User user = findUser(userId);

        preventSelfModification(user, administratorEmail);

        Role newRole = roleRepository.findByName(roleName)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Le rôle " + roleName
                                + " n'existe pas dans la base de données."
                        )
                );

        String currentRole = user.getRole()
                .getName()
                .toUpperCase(Locale.ROOT);

        if (currentRole.equals(roleName)) {
            return toResponse(user);
        }

        if ("ADMIN".equals(currentRole)
                && !"ADMIN".equals(roleName)
                && userRepository.countByRole_Name("ADMIN") <= 1) {

            throw new IllegalStateException(
                    "Impossible de retirer le rôle du dernier administrateur."
            );
        }

        user.setRole(newRole);

        User savedUser = userRepository.save(user);

        invalidateUserSessions(savedUser.getEmail());

        return toResponse(savedUser);
    }

    @Transactional
    public AdminUserResponse changeUserStatus(
            Integer userId,
            String requestedStatus,
            String administratorEmail) {

        if (requestedStatus == null || requestedStatus.isBlank()) {
            throw new IllegalArgumentException(
                    "Le statut est obligatoire."
            );
        }

        validateAdministrator(administratorEmail);

        String newStatus = requestedStatus.trim()
                .toUpperCase(Locale.ROOT);

        if (!ALLOWED_STATUSES.contains(newStatus)) {
            throw new IllegalArgumentException(
                    "Statut non autorisé. Valeurs possibles : ACTIVE, INACTIVE."
            );
        }

        User user = findUser(userId);

        preventSelfModification(user, administratorEmail);

        String currentStatus = user.getStatus() == null
                ? ""
                : user.getStatus().toUpperCase(Locale.ROOT);

        if (currentStatus.equals(newStatus)) {
            return toResponse(user);
        }

        String currentRole = user.getRole()
                .getName()
                .toUpperCase(Locale.ROOT);

        // Protéger le dernier administrateur actif.
        if ("ADMIN".equals(currentRole)
                && "ACTIVE".equals(currentStatus)
                && "INACTIVE".equals(newStatus)
                && userRepository.countByRole_NameAndStatusIgnoreCase(
                        "ADMIN", "ACTIVE") <= 1) {

            throw new IllegalStateException(
                    "Impossible de désactiver le dernier administrateur actif."
            );
        }

        user.setStatus(newStatus);

        User savedUser = userRepository.save(user);

        // Un compte désactivé ne doit plus conserver de session active.
        if ("INACTIVE".equals(newStatus)) {
            invalidateUserSessions(savedUser.getEmail());
        }

        return toResponse(savedUser);
    }

    private void validateAdministrator(String administratorEmail) {

        if (administratorEmail == null || administratorEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Administrateur non identifié."
            );
        }
    }

    private User findUser(Integer userId) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "Identifiant utilisateur invalide."
            );
        }

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Utilisateur introuvable."
                        )
                );
    }

    private void preventSelfModification(
            User user,
            String administratorEmail) {

        if (user.getEmail().equalsIgnoreCase(administratorEmail)) {
            throw new IllegalArgumentException(
                    "Vous ne pouvez pas modifier votre propre compte."
            );
        }
    }

    private void invalidateUserSessions(String email) {

        List<SessionInformation> sessions =
                sessionRegistry.getAllSessions(email, false);

        sessions.forEach(SessionInformation::expireNow);
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
