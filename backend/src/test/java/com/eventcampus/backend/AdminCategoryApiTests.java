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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.List;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AdminCategoryApiTests {
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;
    @MockitoBean AdminCategoryService categories;
    @MockitoBean AccountSessionService sessions;
    MockMvc mvc;
    @BeforeEach void setup() { when(sessions.isCurrent(any())).thenReturn(true); mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build(); }
    MockHttpSession session(String role) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("admin@test", null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        var session = new MockHttpSession(); session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context); return session;
    }
    @Test void adminCanCreateUpdateCheckAndDelete() throws Exception {
        var item = new AdminCategoryService.Item(20, "Name", "Description");
        when(categories.create(any())).thenReturn(item); when(categories.update(eq(20), any())).thenReturn(item);
        when(categories.deletionCheck(20)).thenReturn(new AdminCategoryService.DeletionCheck(20, true, 0, "OK"));
        mvc.perform(post("/api/admin/categories").session(session("ADMIN")).contentType("application/json").content("{\"name\":\"Name\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.category.idCategory").value(20));
        mvc.perform(patch("/api/admin/categories/20").session(session("ADMIN")).contentType("application/json").content("{\"name\":\"Name\"}"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/admin/categories/20/deletion-check").session(session("ADMIN"))).andExpect(jsonPath("$.deletable").value(true));
        mvc.perform(delete("/api/admin/categories/20").session(session("ADMIN"))).andExpect(status().isOk());
        verify(categories).delete(20);
    }
    @Test void allNonAdminsAreForbidden() throws Exception {
        for (var role : List.of("USER", "MODERATOR", "ANONYMOUS")) {
            var session = role.equals("ANONYMOUS") ? new MockHttpSession() : session(role);
            mvc.perform(post("/api/admin/categories").session(session).contentType("application/json").content("{}" )).andExpect(status().isForbidden());
            mvc.perform(patch("/api/admin/categories/1").session(session).contentType("application/json").content("{}" )).andExpect(status().isForbidden());
            mvc.perform(delete("/api/admin/categories/1").session(session)).andExpect(status().isForbidden());
            mvc.perform(get("/api/admin/categories/1/deletion-check").session(session)).andExpect(status().isForbidden());
        }
        verifyNoInteractions(categories);
    }
    @Test void businessConflictIsReturnedClearly() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Catégorie utilisée")).when(categories).delete(1);
        mvc.perform(delete("/api/admin/categories/1").session(session("ADMIN")))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("Catégorie utilisée"));
    }
}
