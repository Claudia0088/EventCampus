package com.eventcampus.backend.service;

import com.eventcampus.backend.entity.User;
import com.eventcampus.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.Set;

@Service
public class AdminUserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final AdminAccountLock lock;
    public AdminUserService(UserRepository users, RoleRepository roles, AdminAccountLock lock) {
        this.users = users; this.roles = roles; this.lock = lock;
    }
    private ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
    private User target(String email, Integer id) {
        if (id == null || id <= 0) throw error(HttpStatus.BAD_REQUEST, "Identifiant invalide.");
        var actor = users.findByEmail(email).orElseThrow(() -> error(HttpStatus.FORBIDDEN, "Compte non autorisé."));
        if (!"ACTIVE".equals(actor.getStatus()) || actor.getRole() == null || !"ADMIN".equals(actor.getRole().getName()))
            throw error(HttpStatus.FORBIDDEN, "Compte non autorisé.");
        return users.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
    }
    private void protect(User user, String actor, boolean removesActiveAdmin) {
        if (user.getEmail().equalsIgnoreCase(actor)) throw error(HttpStatus.CONFLICT, "Vous ne pouvez pas retirer vos propres accès administrateur.");
        if (removesActiveAdmin && users.countByRole_NameAndStatus("ADMIN", "ACTIVE") <= 1)
            throw error(HttpStatus.CONFLICT, "Le dernier administrateur actif doit conserver ses accès.");
    }
    public void role(String actor, Integer id, String value) {
        if (value == null || !Set.of("USER", "MODERATOR", "ADMIN").contains(value)) throw error(HttpStatus.BAD_REQUEST, "Rôle invalide.");
        lock.execute(() -> {
            var user = target(actor, id);
            if (!"ADMIN".equals(value)) protect(user, actor, "ACTIVE".equals(user.getStatus()) && "ADMIN".equals(user.getRole().getName()));
            var role = roles.findByName(value).orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "Rôle absent de la base."));
            users.changeRole(id, role);
            return null;
        });
    }
    public void status(String actor, Integer id, String value) {
        if (value == null || !Set.of("ACTIVE", "INACTIVE").contains(value)) throw error(HttpStatus.BAD_REQUEST, "Statut invalide.");
        lock.execute(() -> {
            var user = target(actor, id);
            if ("INACTIVE".equals(value)) protect(user, actor, "ACTIVE".equals(user.getStatus()) && "ADMIN".equals(user.getRole().getName()));
            users.changeStatus(id, value);
            return null;
        });
    }
}
