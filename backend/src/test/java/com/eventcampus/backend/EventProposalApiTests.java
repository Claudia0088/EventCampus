package com.eventcampus.backend;

import com.eventcampus.backend.dto.*;
import com.eventcampus.backend.service.EventProposalService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(com.eventcampus.backend.controller.UserEventController.class)
@org.springframework.context.annotation.Import({com.eventcampus.backend.config.SecurityConfig.class})
@org.springframework.boot.autoconfigure.ImportAutoConfiguration({org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class, org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class, org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration.class})
class EventProposalApiTests {
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.eventcampus.backend.repository.CategoryRepository categoryRepository;
    @org.junit.jupiter.api.BeforeEach
    void verifyNoDatabaseOrServerKeyComponents() {
        org.junit.jupiter.api.Assertions.assertTrue(context.getBeansOfType(javax.sql.DataSource.class).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(context.getBeansOfType(jakarta.persistence.EntityManagerFactory.class).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(context.getBeansOfType(com.eventcampus.backend.service.QrTokenCodec.class).isEmpty());
    }
    // These HTTP tests construct synthetic sessions; account freshness is tested separately.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.eventcampus.backend.service.AccountSessionService accountSessions;
    @org.junit.jupiter.api.BeforeEach
    void allowSyntheticSessions() {
        org.mockito.Mockito.when(accountSessions.isCurrent(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    }
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;
    @MockitoBean EventProposalService service;
    MockMvc mvc;
    final String valid = """
        {"title":"Test","description":"Description","eventDate":"2099-11-16",
         "eventTime":"14:00:00","location":"Salle","capacity":30,"idCategory":4,
         "id_creator":999,"status":"PUBLIE"}
        """;
    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build(); }
    MockHttpSession session(String role) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("student@test", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
    @Test void userCanCreateUsingSessionIdentity() throws Exception {
        when(service.propose(eq("student@test"), any())).thenReturn(new EventResponse(
                10,"Test","Description",null,null,null,"Salle",30,"EN_ATTENTE",null,null,42,4,"Formation"));
        mvc.perform(post("/api/user/events").session(session("USER")).contentType("application/json").content(valid))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.event.status").value("EN_ATTENTE"))
                .andExpect(jsonPath("$.event.idCreator").value(42));
        verify(service).propose(eq("student@test"), any(EventProposalRequest.class));
    }
    @Test void otherRolesAndAnonymousAreDenied() throws Exception {
        for (var role : List.of("MODERATOR", "ADMIN")) {
            mvc.perform(post("/api/user/events").session(session(role)).contentType("application/json").content(valid))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/user/events/mine").session(session(role))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/user/events").contentType("application/json").content(valid)).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
    @Test void invalidDataNeverReachesService() throws Exception {
        for (var invalid : List.of("{}", valid.replace("\"capacity\":30", "\"capacity\":0"),
                valid.replace("\"title\":\"Test\"", "\"title\":\"   \""),
                valid.replace("2099-11-16", "2020-01-01"), valid.replace("14:00:00", "25:00:00"))) {
            mvc.perform(post("/api/user/events").session(session("USER")).contentType("application/json").content(invalid))
                    .andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }
    @Test void mineIgnoresBrowserCreatorParameters() throws Exception {
        when(service.mine("student@test")).thenReturn(List.of());
        mvc.perform(get("/api/user/events/mine?id_creator=999").session(session("USER")))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verify(service).mine("student@test");
    }
}
