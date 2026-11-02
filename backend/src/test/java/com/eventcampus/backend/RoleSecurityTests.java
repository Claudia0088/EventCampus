package com.eventcampus.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Import(RoleSecurityTests.ProbeController.class)
class RoleSecurityTests {
    // These HTTP tests construct synthetic sessions; account freshness is tested separately.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.eventcampus.backend.service.AccountSessionService accountSessions;
    @org.junit.jupiter.api.BeforeEach
    void allowSyntheticSessions() {
        org.mockito.Mockito.when(accountSessions.isCurrent(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    }
    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy security;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
    }

    private MockHttpSession session(String role) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                "test-role", null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }

    @Test
    void eachSpaceOnlyAcceptsItsRole() throws Exception {
        for (var role : List.of("USER", "MODERATOR", "ADMIN")) {
            for (var space : List.of("user", "moderator", "admin")) {
                mvc.perform(get("/api/" + space + "/security-probe").session(session(role)))
                        .andExpect(status().is(role.equalsIgnoreCase(space) ? 200 : 403));
            }
            mvc.perform(get("/api/events").session(session(role))).andExpect(status().isOk());
        }
    }

    @Test
    void anonymousAndUnknownRolesCannotAccessSpaces() throws Exception {
        for (var space : List.of("user", "moderator", "admin")) {
            mvc.perform(get("/api/" + space + "/security-probe")).andExpect(status().isForbidden());
            mvc.perform(get("/api/" + space + "/security-probe").session(session("UNKNOWN")))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/events").session(session("UNKNOWN"))).andExpect(status().isOk());
    }

    @Test
    void unimplementedRoutesAndEventWritesStayClosed() throws Exception {
        for (var role : List.of("USER", "MODERATOR", "ADMIN")) {
            mvc.perform(get("/api/unassigned").session(session(role))).andExpect(status().isForbidden());
            mvc.perform(post("/api/events").session(session(role))).andExpect(status().isForbidden());
        }
        // The existing public login endpoint remains reachable without a session.
        mvc.perform(post("/api/auth/login").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    // Only loaded by this test: proves the filter blocks a real handler, not just a missing URL.
    @RestController
    static class ProbeController {
        @GetMapping({"/api/user/security-probe", "/api/moderator/security-probe", "/api/admin/security-probe"})
        public String probe() { return "ok"; }
    }
}
