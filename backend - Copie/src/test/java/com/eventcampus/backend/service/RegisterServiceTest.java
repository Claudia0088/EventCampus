package com.eventcampus.backend.service;

import com.eventcampus.backend.dto.RegisterRequest;
import com.eventcampus.backend.entity.Role;
import com.eventcampus.backend.entity.User;
import com.eventcampus.backend.repository.RoleRepository;
import com.eventcampus.backend.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegisterServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final RegisterService registerService =
            new RegisterService(userRepository, roleRepository, null);

    @Test
    void registerSavesUserWithUserRoleAndBcryptPassword() {
        RegisterRequest request = request(
                "  Alice ",
                " Martin ",
                " Alice@Example.com ",
                "secret123"
        );
        Role role = new Role();
        role.setName("USER");

        when(userRepository.existsByEmail("alice@example.com"))
                .thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = registerService.register(request);

        assertEquals("Alice", savedUser.getFirstName());
        assertEquals("Martin", savedUser.getLastName());
        assertEquals("alice@example.com", savedUser.getEmail());
        assertEquals(role, savedUser.getRole());
        assertNotEquals("secret123", savedUser.getPassword());
        assertTrue(new BCryptPasswordEncoder().matches(
                "secret123",
                savedUser.getPassword()
        ));
        verify(userRepository).save(savedUser);
    }

    @Test
    void registerRejectsAnEmailAlreadyInUse() {
        when(userRepository.existsByEmail("alice@example.com"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> registerService.register(request(
                        "Alice", "Martin", "alice@example.com", "secret123"
                ))
        );

        verify(roleRepository, never()).findByName(any());
        verify(userRepository, never()).save(any());
    }

    private RegisterRequest request(
            String firstName,
            String lastName,
            String email,
            String password) {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }
}
