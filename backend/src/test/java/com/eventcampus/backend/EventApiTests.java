package com.eventcampus.backend;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import com.eventcampus.backend.repository.EventRepository;
import org.springframework.data.domain.Sort;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class EventApiTests {
    // These HTTP tests construct synthetic sessions; account freshness is tested separately.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.eventcampus.backend.service.AccountSessionService accountSessions;
    @org.junit.jupiter.api.BeforeEach
    void allowSyntheticSessions() {
        org.mockito.Mockito.when(accountSessions.isCurrent(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    }
    @Autowired private WebApplicationContext context;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private EventRepository eventRepository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain).build();
    }

    @Test
    void moderatorStatsMatchRealMysqlRowsAndRejectOtherRoles() throws Exception {
        var rows = eventRepository.findAll();
        for (var role : List.of("MODERATOR", "USER", "ADMIN")) {
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(new UsernamePasswordAuthenticationToken("stats-reader", null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))));
            var session = new MockHttpSession();
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            var result = mvc.perform(get("/api/moderator/events/stats").session(session));
            if (!role.equals("MODERATOR")) {
                result.andExpect(status().isForbidden());
                continue;
            }
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.pending").value((int) rows.stream().filter(e -> "EN_ATTENTE".equals(e.getStatus())).count()))
                    .andExpect(jsonPath("$.published").value((int) rows.stream().filter(e -> "PUBLIE".equals(e.getStatus())).count()))
                    .andExpect(jsonPath("$.rejected").value((int) rows.stream().filter(e -> "REFUSE".equals(e.getStatus())).count()))
                    .andExpect(jsonPath("$.total").value(rows.size()));
            System.out.println("Real MySQL moderator stats: " + result.andReturn().getResponse().getContentAsString());
        }
        mvc.perform(get("/api/moderator/events/stats")).andExpect(status().isForbidden());
    }

    @Test
    void anonymousCanReadOnlyPublishedEventsFromMysql() throws Exception {
        var published = eventRepository.findAll().stream()
                .filter(event -> "PUBLIE".equals(event.getStatus())).toList();
        var result = mvc.perform(get("/api/events")).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(published.size()));
        for (int i = 0; i < published.size(); i++) {
            result.andExpect(jsonPath("$[" + i + "].status").value("PUBLIE"));
        }
        mvc.perform(get("/api/user/events/mine")).andExpect(status().isForbidden());
        System.out.println("Public MySQL catalogue JSON: " + result.andReturn().getResponse().getContentAsString());
    }

    @Test
    void authenticatedSessionCanReadEventsFromMysql() throws Exception {
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(new UsernamePasswordAuthenticationToken(
                "event-reader", null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                securityContext);
        var result = mvc.perform(get("/api/events").session(session))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].creator").doesNotExist())
                .andExpect(jsonPath("$[*].password").doesNotExist());
        var events = eventRepository.findAll(Sort.by("eventDate", "eventTime", "idEvent"))
                .stream().filter(event -> "PUBLIE".equals(event.getStatus())).toList();
        result.andExpect(jsonPath("$.length()").value(events.size()));
        for (int i = 0; i < events.size(); i++) {
            var event = events.get(i);
            result.andExpect(jsonPath("$[" + i + "].idEvent").value(event.getIdEvent()))
                    .andExpect(jsonPath("$[" + i + "].title").value(event.getTitle()))
                    .andExpect(jsonPath("$[" + i + "].status").value("PUBLIE"))
                    .andExpect(jsonPath("$[" + i + "].eventDate").value(event.getEventDate().toString()))
                    .andExpect(jsonPath("$[" + i + "].capacity").value(event.getCapacity()))
                    .andExpect(jsonPath("$[" + i + "].idCreator").value(event.getCreator().getIdUser()))
                    .andExpect(jsonPath("$[" + i + "].idCategory").value(event.getCategory().getIdCategory()));
        }
        System.out.println("GET /api/events authenticated JSON: "
                + result.andReturn().getResponse().getContentAsString());
    }
}
