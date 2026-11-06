package com.eventcampus.backend.service;

import com.eventcampus.backend.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountSessionService {
    private final UserRepository users;
    public AccountSessionService(UserRepository users) { this.users = users; }
    @Transactional(readOnly = true)
    public boolean isCurrent(Authentication authentication) {
        return users.findByEmail(authentication.getName()).filter(user ->
                "ACTIVE".equals(user.getStatus()) && user.getRole() != null
                && authentication.getAuthorities().size() == 1
                && authentication.getAuthorities().stream().anyMatch(authority ->
                    authority.getAuthority().equals("ROLE_" + user.getRole().getName()))).isPresent();
    }
}
