package com.eventcampus.backend;
import com.eventcampus.backend.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

@SpringBootTest
class QrCodeApiTests {
    @Autowired WebApplicationContext context; @Autowired FilterChainProxy security;
    @MockitoBean QrCodeService codes; @MockitoBean AccountSessionService sessions;
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
        mvc.perform(post("/api/user/registrations/20/qr-code")).andExpect(status().isForbidden());
        verifyNoMoreInteractions(codes);
    }
}
