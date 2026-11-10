package com.eventcampus.backend;

import com.eventcampus.backend.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(com.eventcampus.backend.controller.AdminUserController.class)
@org.springframework.context.annotation.Import({com.eventcampus.backend.config.SecurityConfig.class})
@org.springframework.boot.autoconfigure.ImportAutoConfiguration({org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class, org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class, org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration.class})
class AdminUserApiTests {
    @org.junit.jupiter.api.BeforeEach
    void verifyNoDatabaseOrServerKeyComponents() {
        org.junit.jupiter.api.Assertions.assertTrue(context.getBeansOfType(javax.sql.DataSource.class).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(context.getBeansOfType(jakarta.persistence.EntityManagerFactory.class).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(context.getBeansOfType(com.eventcampus.backend.service.QrTokenCodec.class).isEmpty());
    }
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;
    @MockitoBean AdminUserService users;
    @MockitoBean AccountSessionService sessions;
    MockMvc mvc;
    @BeforeEach void setup() {
        when(sessions.isCurrent(any())).thenReturn(true);
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    }
    MockHttpSession session(String role) {
        var c = SecurityContextHolder.createEmptyContext();
        c.setAuthentication(new UsernamePasswordAuthenticationToken("admin@test", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        var session = new MockHttpSession(); session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, c); return session;
    }
    @Test void adminCanUpdateAndOthersAreForbidden() throws Exception {
        for (var kind : List.of("role", "status")) {
            String body = kind.equals("role") ? "{\"role\":\"MODERATOR\"}" : "{\"status\":\"INACTIVE\"}";
            mvc.perform(patch("/api/admin/users/1/" + kind).session(session("ADMIN")).contentType("application/json").content(body)).andExpect(status().isOk());
            for (var role : List.of("USER", "MODERATOR")) mvc.perform(patch("/api/admin/users/1/" + kind).session(session(role)).contentType("application/json").content(body)).andExpect(status().isForbidden());
            mvc.perform(patch("/api/admin/users/1/" + kind).contentType("application/json").content(body)).andExpect(status().isForbidden());
        }
        verify(users).role("admin@test", 1, "MODERATOR"); verify(users).status("admin@test", 1, "INACTIVE");
    }
    @Test void staleSessionIsInvalidatedBeforeItCanUseAdminApi() throws Exception {
        when(sessions.isCurrent(any())).thenReturn(false);
        var session = session("ADMIN");
        mvc.perform(get("/api/admin/dashboard").session(session)).andExpect(status().isUnauthorized());
        org.junit.jupiter.api.Assertions.assertTrue(session.isInvalid());
        verifyNoInteractions(users);
    }
}
