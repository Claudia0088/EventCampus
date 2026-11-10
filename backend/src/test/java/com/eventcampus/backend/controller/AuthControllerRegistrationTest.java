package com.eventcampus.backend.controller;

import com.eventcampus.backend.entity.User;
import com.eventcampus.backend.service.AuthService;
import com.eventcampus.backend.service.RegisterService;
import com.eventcampus.backend.config.SecurityConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@ImportAutoConfiguration({
        SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
class AuthControllerRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RegisterService registerService;

    @MockitoBean
    private com.eventcampus.backend.service.AccountSessionService accountSessions;

    @Test
    void registerReturnsCreatedWithoutReturningPassword() throws Exception {
        User user = new User();
        user.setEmail("alice@example.com");
        String passwordHash = "$2a$12$hashed-password";
        user.setPassword(passwordHash);
        when(registerService.register(any())).thenReturn(user);

        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstName": "Alice",
                                  "lastName": "Martin",
                                  "email": "alice@example.com",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertFalse(responseBody.contains("secret123"));
        assertFalse(responseBody.contains(passwordHash));
    }

    @Test
    void registerRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstName": "Alice",
                                  "lastName": "Martin",
                                  "email": "not-an-email",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerRejectsMissingRequiredField() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstName": "Alice",
                                  "lastName": "Martin",
                                  "email": "alice@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstName": "Alice",
                                  "lastName": "Martin",
                                  "email": "alice@example.com",
                                  "password": "short"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerReturnsConflictWhenEmailAlreadyExists() throws Exception {
        when(registerService.register(any()))
                .thenThrow(new IllegalArgumentException("duplicate"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstName": "Alice",
                                  "lastName": "Martin",
                                  "email": "alice@example.com",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Cette adresse e-mail est déjà utilisée."
                ));
    }

    @Test
    void registerReturnsGenericServerErrorForInternalFailures() throws Exception {
        when(registerService.register(any()))
                .thenThrow(new IllegalStateException("sensitive database detail"));

        String responseBody = mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content("""
                                {
                                  "firstName": "Alice",
                                  "lastName": "Martin",
                                  "email": "alice@example.com",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(
                        "Impossible de créer le compte pour le moment."
                ))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertFalse(responseBody.contains("sensitive database detail"));
    }

    @Test
    void registrationIsPublicButOtherAuthRoutesRemainProtected()
            throws Exception {
        int status = mockMvc.perform(get("/api/auth/me"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertFalse(status >= 200 && status < 300);
    }
}
