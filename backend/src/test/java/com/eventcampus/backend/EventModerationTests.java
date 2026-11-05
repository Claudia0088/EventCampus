package com.eventcampus.backend;

import com.eventcampus.backend.entity.*;
import com.eventcampus.backend.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.time.*;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class EventModerationTests {
    // These HTTP tests construct synthetic sessions; account freshness is tested separately.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.eventcampus.backend.service.AccountSessionService accountSessions;
    @org.junit.jupiter.api.BeforeEach
    void allowSyntheticSessions() {
        org.mockito.Mockito.when(accountSessions.isCurrent(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    }
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;
    @MockitoBean EventRepository events;
    @MockitoBean UserRepository users;
    MockMvc mvc;
    Event event;

    MockHttpSession session(String role) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("student@test", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }

    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
        var creator = new User(); creator.setIdUser(42); creator.setFirstName("Test");
        creator.setLastName("Student"); creator.setPassword("secret"); creator.setStatus("ACTIVE");
        var category = new Category(); category.setIdCategory(4); category.setName("Formation");
        event = new Event(); event.setIdEvent(10); event.setTitle("Atelier Java");
        event.setDescription("Description"); event.setStatus("EN_ATTENTE");
        event.setCreator(creator); event.setCategory(category); event.setCapacity(30);
        event.setLocation("Salle"); event.setEventDate(LocalDate.of(2099, 11, 16)); event.setEventTime(LocalTime.NOON);
        when(users.findByEmail("student@test")).thenReturn(Optional.of(creator));
        when(events.existsById(10)).thenReturn(true);
        when(events.findAllByStatus(eq("EN_ATTENTE"), any())).thenAnswer(i ->
                "EN_ATTENTE".equals(event.getStatus()) ? List.of(event) : List.of());
        when(events.findByStatus(eq("PUBLIE"), any())).thenAnswer(i ->
                "PUBLIE".equals(event.getStatus()) ? List.of(event) : List.of());
        when(events.findByCreator_IdUser(eq(42), any())).thenReturn(List.of(event));
        // Simulate the conditional UPDATE without executing writes in local MySQL.
        when(events.decidePending(eq(10), anyString())).thenAnswer(i -> {
            if (!"EN_ATTENTE".equals(event.getStatus())) return 0;
            event.setStatus(i.getArgument(1)); return 1;
        });
    }

    @Test void pendingContainsDetailsAndSafeCreator() throws Exception {
        mvc.perform(get("/api/moderator/events/pending").session(session("MODERATOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].event.title").value("Atelier Java"))
                .andExpect(jsonPath("$[0].event.categoryName").value("Formation"))
                .andExpect(jsonPath("$[0].creator.firstName").value("Test"))
                .andExpect(jsonPath("$[0].creator.password").doesNotExist());
    }

    @Test void statsTotalIncludesUnlistedStatuses() throws Exception {
        var counts = mock(EventRepository.StatsCounts.class);
        when(counts.getPending()).thenReturn(2L);
        when(counts.getPublished()).thenReturn(8L);
        when(counts.getRejected()).thenReturn(1L);
        // One additional event has another status: total must not be rebuilt from known statuses.
        when(counts.getTotal()).thenReturn(12L);
        when(events.getEventStats()).thenReturn(counts);
        mvc.perform(get("/api/moderator/events/stats").session(session("MODERATOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.pending").value(2))
                .andExpect(jsonPath("$.published").value(8)).andExpect(jsonPath("$.rejected").value(1))
                .andExpect(jsonPath("$.total").value(12));
        verify(events).getEventStats();
    }

    @Test void approvePublishesOnceAndRemainsInMine() throws Exception {
        mvc.perform(patch("/api/moderator/events/10/approve").session(session("MODERATOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PUBLIE"));
        mvc.perform(patch("/api/moderator/events/10/reject").session(session("MODERATOR")))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/moderator/events/10/approve").session(session("MODERATOR")))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/events").session(session("USER")))
                .andExpect(jsonPath("$[0].status").value("PUBLIE"));
        mvc.perform(get("/api/user/events/mine").session(session("USER")))
                .andExpect(jsonPath("$[0].status").value("PUBLIE"));
        mvc.perform(get("/api/moderator/events/pending").session(session("MODERATOR")))
                .andExpect(content().json("[]"));
    }

    @Test void rejectionStaysPrivateAndRemainsInMine() throws Exception {
        mvc.perform(patch("/api/moderator/events/10/reject").session(session("MODERATOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REFUSE"));
        mvc.perform(patch("/api/moderator/events/10/reject").session(session("MODERATOR")))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/events").session(session("USER"))).andExpect(content().json("[]"));
        mvc.perform(get("/api/user/events/mine").session(session("USER")))
                .andExpect(jsonPath("$[0].status").value("REFUSE"));
    }

    @Test void missingAndInvalidIdsHaveClearErrors() throws Exception {
        mvc.perform(patch("/api/moderator/events/999/approve").session(session("MODERATOR")))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").exists());
        for (var id : List.of("0", "abc")) {
            mvc.perform(patch("/api/moderator/events/" + id + "/reject").session(session("MODERATOR")))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test void userAdminAndAnonymousCannotModerate() throws Exception {
        for (var role : List.of("USER", "ADMIN")) {
            mvc.perform(get("/api/moderator/events/pending").session(session(role))).andExpect(status().isForbidden());
            for (var action : List.of("approve", "reject")) {
                mvc.perform(patch("/api/moderator/events/10/" + action).session(session(role)))
                        .andExpect(status().isForbidden());
            }
        }
        mvc.perform(get("/api/moderator/events/pending")).andExpect(status().isForbidden());
        for (var action : List.of("approve", "reject")) {
            mvc.perform(patch("/api/moderator/events/10/" + action)).andExpect(status().isForbidden());
        }
        verifyNoInteractions(events);
    }
}
