package com.eventcampus.backend;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.util.List;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class AdminDashboardTests {
    // These HTTP tests construct synthetic sessions; account freshness is tested separately.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.eventcampus.backend.service.AccountSessionService accountSessions;
    @org.junit.jupiter.api.BeforeEach
    void allowSyntheticSessions() {
        org.mockito.Mockito.when(accountSessions.isCurrent(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    }
    @Autowired WebApplicationContext context;
    @Autowired FilterChainProxy security;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired EventRepository events;
    @Autowired com.eventcampus.backend.repository.AdminAccountLock accountLock;
    @Autowired com.eventcampus.backend.repository.CategoryWriteLock categoryLock;
    @Autowired com.eventcampus.backend.repository.RegistrationWriteLock registrationLock;
    @Test void registrationLockSerializesSameEventWithoutWritingData() throws Exception {
        assertSerializes(work -> registrationLock.execute(2147483647, work));
    }

    @Test void mysqlLockSerializesConcurrentTransactionsWithoutChangingData() throws Exception {
        assertSerializes(work -> accountLock.execute(work));
    }

    @Test void categoryLockSerializesWritersWithoutChangingData() throws Exception {
        assertSerializes(work -> categoryLock.execute(work));
    }

    private void assertSerializes(java.util.function.Function<java.util.function.Supplier<String>, String> locked) throws Exception {
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var secondStarted = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> locked.apply(() -> {
                entered.countDown();
                try { if (!release.await(5, java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException(); }
                catch (InterruptedException error) { throw new IllegalStateException(error); }
                return "first";
            }));
            org.junit.jupiter.api.Assertions.assertTrue(entered.await(5, java.util.concurrent.TimeUnit.SECONDS));
            var second = executor.submit(() -> { secondStarted.countDown(); return locked.apply(() -> "second"); });
            org.junit.jupiter.api.Assertions.assertTrue(secondStarted.await(5, java.util.concurrent.TimeUnit.SECONDS));
            org.junit.jupiter.api.Assertions.assertThrows(java.util.concurrent.TimeoutException.class,
                    () -> second.get(150, java.util.concurrent.TimeUnit.MILLISECONDS));
            release.countDown();
            org.junit.jupiter.api.Assertions.assertEquals("first", first.get(5, java.util.concurrent.TimeUnit.SECONDS));
            org.junit.jupiter.api.Assertions.assertEquals("second", second.get(5, java.util.concurrent.TimeUnit.SECONDS));
        } finally { release.countDown(); executor.shutdownNow(); }
    }
    MockMvc mvc;
    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).addFilters(security).build(); }
    MockHttpSession session(String role) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken("admin-test", null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))));
        var session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }
    @Test void dashboardMatchesMysqlWithoutExposingPasswords() throws Exception {
        var accounts = users.findAll();
        var allEvents = events.findAll();
        var result = mvc.perform(get("/api/admin/dashboard").session(session("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stats.totalUsers").value(accounts.size()))
                .andExpect(jsonPath("$.stats.userAccounts").value((int) accounts.stream().filter(u -> "USER".equals(u.getRole().getName())).count()))
                .andExpect(jsonPath("$.stats.moderatorAccounts").value((int) accounts.stream().filter(u -> "MODERATOR".equals(u.getRole().getName())).count()))
                .andExpect(jsonPath("$.stats.adminAccounts").value((int) accounts.stream().filter(u -> "ADMIN".equals(u.getRole().getName())).count()))
                .andExpect(jsonPath("$.stats.totalEvents").value(allEvents.size()))
                .andExpect(jsonPath("$.stats.publishedEvents").value((int) allEvents.stream().filter(e -> "PUBLIE".equals(e.getStatus())).count()))
                .andExpect(jsonPath("$.stats.pendingEvents").value((int) allEvents.stream().filter(e -> "EN_ATTENTE".equals(e.getStatus())).count()))
                .andExpect(jsonPath("$.users.length()").value(accounts.size()))
                .andExpect(jsonPath("$.categories.length()").value((int) categories.count()))
                .andExpect(jsonPath("$.events.length()").value(allEvents.size()))
                .andExpect(jsonPath("$.users[*].password").doesNotExist());
        for (int i = 0; i < accounts.size(); i++) {
            result.andExpect(jsonPath("$.users[" + i + "].email").value(accounts.get(i).getEmail()));
        }
    }
    @Test void onlyAdminCanReadAndNoWriteHandlerExists() throws Exception {
        mvc.perform(get("/api/admin/dashboard")).andExpect(status().isForbidden());
        for (var role : List.of("USER", "MODERATOR")) {
            mvc.perform(get("/api/admin/dashboard").session(session(role))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/api/admin/dashboard").session(session("ADMIN"))).andExpect(status().isMethodNotAllowed());
    }
}
