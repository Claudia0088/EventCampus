package com.eventcampus.backend;

import com.eventcampus.backend.entity.Category;
import com.eventcampus.backend.entity.Event;
import com.eventcampus.backend.entity.User;
import com.eventcampus.backend.repository.EventRepository;
import com.eventcampus.backend.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventServiceTests {
    @Test
    void catalogueOnlyExposesExactPublishedStatusAndCategoryName() {
        var repository = mock(EventRepository.class);
        var category = new Category();
        category.setIdCategory(4);
        category.setName("Formation");
        var creator = new User();
        creator.setIdUser(1);
        creator.setPassword("must-not-be-exposed");
        var published = new Event();
        published.setIdEvent(10);
        published.setTitle("Titre issu du repository");
        published.setStatus("PUBLIE");
        published.setCategory(category);
        published.setCreator(creator);
        var pending = new Event();
        pending.setStatus("EN_ATTENTE");
        var unknown = new Event();
        unknown.setStatus("publie");
        var refused = new Event();
        refused.setStatus("REFUSE");
        var sort = Sort.by("eventDate", "eventTime", "idEvent");
        when(repository.findByStatus("PUBLIE", sort)).thenReturn(List.of(published, pending, unknown, refused));

        var result = new EventService(repository).getAllEvents();

        assertEquals(1, result.size());
        assertEquals(10, result.getFirst().idEvent());
        assertEquals("Titre issu du repository", result.getFirst().title());
        assertEquals("Formation", result.getFirst().categoryName());
        assertEquals(1, result.getFirst().idCreator());
        verify(repository).findByStatus("PUBLIE", sort);
    }
}
