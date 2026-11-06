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

@WebMvcTest(com.eventcampus.backend.controller.RegistrationController.class)
@org.springframework.context.annotation.Import({com.eventcampus.backend.config.SecurityConfig.class})
@org.springframework.boot.autoconfigure.ImportAutoConfiguration({org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration.class, org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration.class, org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration.class})
class RegistrationApiTests {
    @org.junit.jupiter.api.BeforeEach
    void verifyNoDatabaseOrServerKeyComponents() {
        org.junit.jupiter.api.Assertions.assertTrue(context.getBeansOfType(javax.sql.DataSource.class).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(context.getBeansOfType(jakarta.persistence.EntityManagerFactory.class).isEmpty());
        org.junit.jupiter.api.Assertions.assertTrue(context.getBeansOfType(com.eventcampus.backend.service.QrTokenCodec.class).isEmpty());
    }
    @Autowired WebApplicationContext context; @Autowired FilterChainProxy security;
    @MockitoBean RegistrationService registrations; @MockitoBean AccountSessionService sessions;
    MockMvc mvc;
    @BeforeEach void setup(){when(sessions.isCurrent(any())).thenReturn(true);mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();}
    MockHttpSession session(String role){var c=SecurityContextHolder.createEmptyContext();c.setAuthentication(new UsernamePasswordAuthenticationToken("student@test",null,List.of(new SimpleGrantedAuthority("ROLE_"+role))));var s=new MockHttpSession();s.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,c);return s;}
    @Test void studentCanRegisterReadAndCancelUsingSessionOnly() throws Exception {
        when(registrations.mine("student@test")).thenReturn(List.of());
        mvc.perform(post("/api/user/events/10/registrations?id_user=99").session(session("USER"))).andExpect(status().isCreated());
        mvc.perform(get("/api/user/registrations/mine?id_user=99").session(session("USER"))).andExpect(status().isOk());
        mvc.perform(patch("/api/user/registrations/1/cancel").session(session("USER"))).andExpect(status().isOk());
        verify(registrations).register("student@test",10);verify(registrations).mine("student@test");verify(registrations).cancel("student@test",1);
    }
    @Test void privateApisRejectAnonymousAndOtherRoles() throws Exception {
        for(var role:List.of("ADMIN","MODERATOR","ANONYMOUS")){var s=role.equals("ANONYMOUS")?new MockHttpSession():session(role);
            mvc.perform(post("/api/user/events/10/registrations").session(s)).andExpect(status().isForbidden());
            mvc.perform(get("/api/user/registrations/mine").session(s)).andExpect(status().isForbidden());
            mvc.perform(patch("/api/user/registrations/1/cancel").session(s)).andExpect(status().isForbidden());
        }verifyNoInteractions(registrations);
    }
    @Test void availabilityIsPublicWithoutPersonalData() throws Exception {
        when(registrations.availability()).thenReturn(List.of(new RegistrationService.Availability(10,30,12,true)));
        mvc.perform(get("/api/events/availability")).andExpect(status().isOk()).andExpect(jsonPath("$[0].remaining").value(12));
    }
}
