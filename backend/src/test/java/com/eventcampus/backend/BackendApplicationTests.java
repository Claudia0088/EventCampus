package com.eventcampus.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BackendApplicationTests {
    // These HTTP tests construct synthetic sessions; account freshness is tested separately.
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.eventcampus.backend.service.AccountSessionService accountSessions;
    @org.junit.jupiter.api.BeforeEach
    void allowSyntheticSessions() {
        org.mockito.Mockito.when(accountSessions.isCurrent(org.mockito.ArgumentMatchers.any())).thenReturn(true);
    }

	@Test
	void contextLoads() {
	}

}
