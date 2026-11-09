
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

    // Récupérer tous les utilisateurs
    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Récupérer les rôles disponibles
    @Transactional(readOnly = true)
    public List<String> listRoles() {

        return roleRepository.findAll()
                .stream()
                .map(Role::getName)
                .sorted()
                .toList();
    }

    // Modifier le rôle d'un utilisateur
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

        if (administratorEmail == null || administratorEmail.isBlank()) {
            throw new IllegalArgumentException(
                    "Administrateur non identifié."
            );
        }

        String roleName = requestedRole
                .trim()
                .toUpperCase(Locale.ROOT);

        // Vérifier que le rôle est autorisé
        if (!ALLOWED_ROLES.contains(roleName)) {
            throw new IllegalArgumentException(
                    "Rôle non autorisé. Valeurs possibles : USER, MODERATOR, ADMIN."
            );
        }

        // Rechercher l'utilisateur
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Utilisateur introuvable."
                        )
                );

        // Empêcher l'administrateur de modifier son propre rôle
        if (user.getEmail().equalsIgnoreCase(administratorEmail)) {
            throw new IllegalArgumentException(
                    "Vous ne pouvez pas modifier votre propre rôle."
            );
        }

        // Rechercher le rôle dans la base de données
        Role newRole = roleRepository.findByName(roleName)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Le rôle " + roleName + " n'existe pas dans la base de données."
                        )
                );

        String currentRole = user.getRole()
                .getName()
                .toUpperCase(Locale.ROOT);

        // Éviter un changement inutile
        if (currentRole.equals(roleName)) {
            return toResponse(user);
        }

        // Empêcher la rétrogradation du dernier administrateur
        if ("ADMIN".equals(currentRole)
                && !"ADMIN".equals(roleName)
                && userRepository.countByRole_Name("ADMIN") <= 1) {

            throw new IllegalStateException(
                    "Impossible de retirer le rôle du dernier administrateur."
            );
        }

        // Enregistrer le nouveau rôle
        user.setRole(newRole);

        User savedUser = userRepository.save(user);

        // Invalider les sessions de l'utilisateur concerné
        List<SessionInformation> sessions =
                sessionRegistry.getAllSessions(
                        savedUser.getEmail(),
                        false
                );

        sessions.forEach(SessionInformation::expireNow);

        return toResponse(savedUser);
    }

    // Transformer une entité User en réponse sécurisée
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

    // Exception si l'utilisateur n'existe pas
    public static class UserNotFoundException
            extends RuntimeException {

        public UserNotFoundException(String message) {
            super(message);
        }
    }
}
