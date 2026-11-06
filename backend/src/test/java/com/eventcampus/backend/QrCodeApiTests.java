package com.eventcampus.backend;
import com.eventcampus.backend.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import com.eventcampus.backend.config.SecurityConfig;
import com.eventcampus.backend.controller.QrCodeController;
import com.eventcampus.backend.controller.QrCheckInController;
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

@SpringJUnitConfig(QrCodeApiTests.WebConfig.class)
@WebAppConfiguration
class QrCodeApiTests {
    // No Boot auto-configuration, JPA, datasource or connection to MySQL.
    @Configuration @EnableWebMvc @EnableWebSecurity
    @Import({SecurityConfig.class, QrCodeController.class, QrCheckInController.class, ErrorEndpoint.class})
    static class WebConfig {}
    @org.springframework.web.bind.annotation.RestController
    static class ErrorEndpoint {
        @org.springframework.web.bind.annotation.RequestMapping("/error")
        org.springframework.http.ResponseEntity<?> error() { return org.springframework.http.ResponseEntity.status(500).body(java.util.Map.of("message", "Erreur interne.")); }
    }
    @Autowired WebApplicationContext context; @Autowired FilterChainProxy security;
    @MockitoBean QrCodeService codes; @MockitoBean AccountSessionService sessions;
    @MockitoBean QrCheckInService checkIn;
    MockMvc mvc;
    @BeforeEach void setup(){when(sessions.isCurrent(any())).thenReturn(true);mvc=MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();}
    MockHttpSession session(String role){var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(new UsernamePasswordAuthenticationToken("student@test",null,List.of(new SimpleGrantedAuthority("ROLE_"+role))));var session=new MockHttpSession();session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,context);return session;}
    @Test void onlyStudentCanRetrievePrivatePngWithoutCaching() throws Exception {
        when(codes.png("student@test",20)).thenReturn(new byte[]{1,2,3});
        mvc.perform(post("/api/user/registrations/20/qr-code?id_user=999").session(session("USER")))
                .andExpect(status().isOk()).andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control","no-store"));
        verify(codes).png("student@test",20);
        for(var role:List.of("ADMIN","MODERATOR"))mvc.perform(post("/api/user/registrations/20/qr-code").session(session(role))).andExpect(status().isForbidden());
        mvc.perform(post("/api/user/registrations/20/qr-code")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
        verifyNoMoreInteractions(codes);
    }
    @Test void missingKeyIs503JsonNot403() throws Exception {
        when(codes.png("student@test",20)).thenThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE, "Clé QR serveur indisponible. Contactez l'administration."));
        mvc.perform(post("/api/user/registrations/20/qr-code").session(session("USER")).accept("image/png", "application/json"))
                .andExpect(status().isServiceUnavailable()).andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.message").value("Clé QR serveur indisponible. Contactez l'administration."));
    }
    @Test void errorDispatchPreservesErrorButDirectAccessRemainsForbidden() throws Exception {
        mvc.perform(post("/error").servletPath("/error").with(request -> { request.setDispatcherType(jakarta.servlet.DispatcherType.ERROR); return request; }).session(session("USER")))
                .andExpect(status().isInternalServerError());
        mvc.perform(post("/error").servletPath("/error").session(session("USER"))).andExpect(status().isForbidden());
    }
    @Test void invalidIdAndOtherOwnersAreNotExposed() throws Exception {
        mvc.perform(post("/api/user/registrations/bad/qr-code").session(session("USER")).accept("image/png", "application/json"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").exists());
        when(codes.png("student@test",21)).thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Inscription introuvable."));
        mvc.perform(post("/api/user/registrations/21/qr-code").session(session("USER")))
                .andExpect(status().isNotFound());
    }
    @Test void staleSessionIs401() throws Exception {
        when(sessions.isCurrent(any())).thenReturn(false);
        mvc.perform(post("/api/user/registrations/20/qr-code").session(session("USER")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").exists());
        verifyNoInteractions(codes);
    }
    @Test void contextHasNoDatasourceAndOtherAnonymousRoutesKeepExistingPermissions() throws Exception {
        org.junit.jupiter.api.Assertions.assertTrue(context.getBeansOfType(javax.sql.DataSource.class).isEmpty());
        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isForbidden());
        mvc.perform(get("/api/user/registrations/mine")).andExpect(status().isForbidden());
    }
    @Test void onlyAdminAndModeratorCanScanAndIntentIsRequired() throws Exception {
        String body = "{\"eventId\":10,\"token\":\"EC1." + "A".repeat(43) + "\"}";
        when(checkIn.scan(anyString(),eq(10),anyString())).thenReturn(new QrCheckInService.Result("VALID","Présence enregistrée.",20,"Demo",java.time.LocalDateTime.now()));
        for (String role : List.of("ADMIN","MODERATOR")) {
            mvc.perform(post("/api/check-in/scan").session(session(role)).header("X-EventCampus-Scan","1").contentType("application/json").content(body))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("VALID"));
        }
        mvc.perform(post("/api/check-in/scan").session(session("USER")).header("X-EventCampus-Scan","1").contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/check-in/scan").header("X-EventCampus-Scan","1").contentType("application/json").content(body)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/check-in/scan").session(session("ADMIN")).contentType("application/json").content(body)).andExpect(status().isBadRequest());
        verify(checkIn,times(2)).scan(anyString(),eq(10),anyString());
    }
    @Test void corsAllowsLocalPreflightAndRejectsUnknownOrigins() throws Exception {
        mvc.perform(options("/api/check-in/scan").header("Origin","http://localhost:5500").header("Access-Control-Request-Method","POST")
                .header("Access-Control-Request-Headers","content-type,x-eventcampus-scan"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Credentials","true"));
        mvc.perform(options("/api/check-in/scan").header("Origin","https://untrusted.example").header("Access-Control-Request-Method","POST"))
                .andExpect(status().isForbidden());
    }
    @Test void imageScanIsAuthorizedAndDecodedWithoutRealDatabase() throws Exception {
        String token = "EC1." + "A".repeat(43);
        when(checkIn.scan("student@test",10,token)).thenReturn(new QrCheckInService.Result("VALID","Présence enregistrée.",20,"Demo",java.time.LocalDateTime.now()));
        var file = new org.springframework.mock.web.MockMultipartFile("image","qr.png","image/png",QrCodeService.render(token));
        mvc.perform(multipart("/api/check-in/scan-image").file(file).param("eventId","10").header("X-EventCampus-Scan","1").session(session("MODERATOR")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("VALID"));
        mvc.perform(multipart("/api/check-in/scan-image").file(file).param("eventId","10").header("X-EventCampus-Scan","1").session(session("USER")))
                .andExpect(status().isForbidden());
    }
}
