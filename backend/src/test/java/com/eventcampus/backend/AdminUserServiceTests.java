package com.eventcampus.backend;

import com.eventcampus.backend.entity.*;
import com.eventcampus.backend.repository.*;
import com.eventcampus.backend.service.*;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminUserServiceTests {
    UserRepository users = mock(UserRepository.class);
    RoleRepository roles = mock(RoleRepository.class);
    AdminAccountLock lock = mock(AdminAccountLock.class);
    AdminUserService service = new AdminUserService(users, roles, lock);
    User actor, target;
    Role role(String name) { var r = new Role(); r.setName(name); return r; }
    User user(int id, String email, String role) {
        var u = new User(); u.setIdUser(id); u.setEmail(email); u.setRole(role(role)); u.setStatus("ACTIVE"); return u;
    }
    @BeforeEach void setup() {
        when(lock.execute(any())).thenAnswer(call -> ((Supplier<?>)call.getArgument(0)).get());
        actor = user(7, "admin@test", "ADMIN"); target = user(1, "user@test", "USER");
        when(users.findByEmail("admin@test")).thenReturn(Optional.of(actor));
        when(users.findById(1)).thenReturn(Optional.of(target)); when(users.findById(7)).thenReturn(Optional.of(actor));
        for (var name : List.of("USER", "MODERATOR", "ADMIN")) when(roles.findByName(name)).thenReturn(Optional.of(role(name)));
    }
    @Test void updatesOnlyRequestedColumn() {
        service.role("admin@test", 1, "MODERATOR");
        verify(users).changeRole(eq(1), argThat(r -> "MODERATOR".equals(r.getName())));
        verify(users, never()).save(any()); verify(users, never()).changeStatus(any(), any());
        assertEquals("user@test", target.getEmail()); assertEquals("ACTIVE", target.getStatus());
    }
    @Test void enablesAndDisables() {
        service.status("admin@test", 1, "INACTIVE"); service.status("admin@test", 1, "ACTIVE");
        verify(users).changeStatus(1, "INACTIVE"); verify(users).changeStatus(1, "ACTIVE");
        verify(users, never()).save(any()); verify(users, never()).changeRole(any(), any());
    }
    @Test void rejectsSelfChangesAndLastActiveAdmin() {
        assertThrows(ResponseStatusException.class, () -> service.role("admin@test", 7, "USER"));
        assertThrows(ResponseStatusException.class, () -> service.status("admin@test", 7, "INACTIVE"));
        target.setRole(role("ADMIN")); when(users.countByRole_NameAndStatus("ADMIN", "ACTIVE")).thenReturn(1L);
        assertThrows(ResponseStatusException.class, () -> service.role("admin@test", 1, "USER"));
        assertThrows(ResponseStatusException.class, () -> service.status("admin@test", 1, "INACTIVE"));
        verify(users, never()).changeRole(any(), any()); verify(users, never()).changeStatus(any(), any());
    }
    @Test void rejectsInvalidMissingAndUnauthorizedRequests() {
        assertThrows(ResponseStatusException.class, () -> service.role("admin@test", 1, "ROOT"));
        assertThrows(ResponseStatusException.class, () -> service.status("admin@test", 1, "ACTIF"));
        assertThrows(ResponseStatusException.class, () -> service.role("admin@test", 0, "USER"));
        assertThrows(ResponseStatusException.class, () -> service.status("admin@test", 999, "ACTIVE"));
        actor.setRole(role("USER"));
        assertThrows(ResponseStatusException.class, () -> service.role("admin@test", 1, "ADMIN"));
        verify(users, never()).changeRole(any(), any()); verify(users, never()).changeStatus(any(), any());
    }
    @Test void accountFreshnessRejectsOldRoleAndDisabledAccount() {
        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "admin@test", null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN")));
        var sessions = new AccountSessionService(users);
        assertTrue(sessions.isCurrent(auth));
        actor.setRole(role("USER")); assertFalse(sessions.isCurrent(auth));
        actor.setRole(role("ADMIN")); actor.setStatus("INACTIVE"); assertFalse(sessions.isCurrent(auth));
    }
    @Test void disabledAccountCannotLogin() {
        actor.setStatus("INACTIVE"); actor.setPassword("unused");
        var auth = new AuthService(users);
        assertNull(auth.login("admin@test", "unused"));
        verify(users, never()).save(any());
    }
}
