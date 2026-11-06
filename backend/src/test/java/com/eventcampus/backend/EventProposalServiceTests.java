package com.eventcampus.backend;

import com.eventcampus.backend.dto.EventProposalRequest;
import com.eventcampus.backend.entity.*;
import com.eventcampus.backend.repository.*;
import com.eventcampus.backend.service.EventProposalService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventProposalServiceTests {
    private final EventRepository events = mock(EventRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final EntityManager em = mock(EntityManager.class);
    private final CategoryWriteLock categoryLock = mock(CategoryWriteLock.class);
    private final EventProposalService service = new EventProposalService(events, users, categories, em, categoryLock);
    @org.junit.jupiter.api.BeforeEach
    void setupCategoryLock() {
        when(categoryLock.execute(any())).thenAnswer(call -> ((java.util.function.Supplier<?>) call.getArgument(0)).get());
    }
    private EventProposalRequest request(Integer category, LocalDate date) {
        return new EventProposalRequest(" Test ", " Description ", date, LocalTime.NOON, " Salle ", 30, category);
    }
    private void user(String email, int id) {
        var user = new User(); user.setIdUser(id); user.setStatus("ACTIVE");
        when(users.findByEmail(email)).thenReturn(Optional.of(user));
    }

    @Test
    void creatorComesFromSessionAndStatusIsAlwaysPending() {
        user("student@test", 42);
        var category = new Category(); category.setIdCategory(4); category.setName("Formation");
        when(categories.findById(4)).thenReturn(Optional.of(category));
        var response = service.propose("student@test", request(4, LocalDate.now().plusDays(2)));
        var saved = ArgumentCaptor.forClass(Event.class);
        verify(events).saveAndFlush(saved.capture());
        assertEquals(42, saved.getValue().getCreator().getIdUser());
        assertEquals("EN_ATTENTE", response.status());
        assertEquals("Test", response.title());
        assertNull(saved.getValue().getImage());
        verify(em).refresh(saved.getValue());
    }

    @Test
    void rejectsMissingCategoryAndPastTimeWithoutWriting() {
        user("student@test", 42);
        when(categories.findById(4)).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class, () -> service.propose("student@test", request(4, LocalDate.now().plusDays(2))));
        when(categories.findById(4)).thenReturn(Optional.of(new Category()));
        assertThrows(ResponseStatusException.class, () -> service.propose("student@test", request(4, LocalDate.now().minusDays(1))));
        verify(events, never()).saveAndFlush(any());
    }

    @Test
    void eachStudentIsQueriedByTheirOwnId() {
        user("first@test", 42); user("second@test", 43);
        when(events.findByCreator_IdUser(anyInt(), any(Sort.class))).thenReturn(List.of());
        service.mine("first@test"); service.mine("second@test");
        verify(events).findByCreator_IdUser(eq(42), any(Sort.class));
        verify(events).findByCreator_IdUser(eq(43), any(Sort.class));
    }
}
