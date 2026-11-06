package com.eventcampus.backend;

import com.eventcampus.backend.entity.Category;
import com.eventcampus.backend.repository.*;
import com.eventcampus.backend.service.AdminCategoryService;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminCategoryServiceTests {
    CategoryRepository categories = mock(CategoryRepository.class);
    EventRepository events = mock(EventRepository.class);
    CategoryWriteLock lock = mock(CategoryWriteLock.class);
    AdminCategoryService service = new AdminCategoryService(categories, events, lock);
    Category category(int id, String name) { var c = new Category(); c.setIdCategory(id); c.setName(name); return c; }
    @BeforeEach void setup() {
        when(lock.execute(any())).thenAnswer(call -> ((Supplier<?>)call.getArgument(0)).get());
        when(categories.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
    }
    @Test void createsNormalizedCategory() {
        when(categories.findAll()).thenReturn(List.of(category(1, "Sport")));
        var result = service.create(new AdminCategoryService.Input("  Vie\t du   campus ", " Description "));
        assertEquals("Vie du campus", result.name()); assertEquals("Description", result.description());
        verify(lock).execute(any()); verify(categories).saveAndFlush(any()); verifyNoInteractions(events);
    }
    @Test void rejectsCreationAndUpdateDuplicatesIgnoringCaseAndWhitespace() {
        var existing = category(1, " Vie  du campus "); var other = category(2, "Sport");
        when(categories.findAll()).thenReturn(List.of(existing, other)); when(categories.findById(2)).thenReturn(Optional.of(other));
        assertThrows(ResponseStatusException.class, () -> service.create(new AdminCategoryService.Input("vie du CAMPUS", null)));
        assertThrows(ResponseStatusException.class, () -> service.update(2, new AdminCategoryService.Input(" VIE\tdu campus ", null)));
        verify(categories, never()).saveAndFlush(any());
    }
    @Test void updatesOnlyAllowedFieldsAndKeepsId() {
        var c = category(4, "Formation"); when(categories.findById(4)).thenReturn(Optional.of(c)); when(categories.findAll()).thenReturn(List.of(c));
        var result = service.update(4, new AdminCategoryService.Input(" Formation ", "Nouvelle description"));
        assertEquals(4, result.idCategory()); assertEquals("Nouvelle description", result.description());
        verify(categories).saveAndFlush(c); verifyNoInteractions(events);
    }
    @Test void deletesUnusedButRejectsUsedCategoryForAnyStatus() {
        var c = category(4, "Formation"); when(categories.findById(4)).thenReturn(Optional.of(c));
        when(events.countByCategory_IdCategory(4)).thenReturn(1L);
        assertFalse(service.deletionCheck(4).deletable());
        assertThrows(ResponseStatusException.class, () -> service.delete(4)); verify(categories, never()).delete(any());
        when(events.countByCategory_IdCategory(4)).thenReturn(0L);
        assertTrue(service.deletionCheck(4).deletable()); service.delete(4); verify(categories).delete(c); verify(categories).flush();
    }
    @Test void finalCheckRejectsReferenceAddedAfterPreflight() {
        when(categories.findById(4)).thenReturn(Optional.of(category(4, "Formation")));
        when(events.countByCategory_IdCategory(4)).thenReturn(0L, 1L);
        assertTrue(service.deletionCheck(4).deletable());
        assertThrows(ResponseStatusException.class, () -> service.delete(4)); verify(categories, never()).delete(any());
    }
    @Test void rejectsInvalidAndMissingData() {
        for (var name : List.of("", "   ", "a".repeat(101))) assertThrows(ResponseStatusException.class,
                () -> service.create(new AdminCategoryService.Input(name, null)));
        assertThrows(ResponseStatusException.class, () -> service.create(new AdminCategoryService.Input("Name", "a".repeat(256))));
        assertThrows(ResponseStatusException.class, () -> service.delete(0));
        assertThrows(ResponseStatusException.class, () -> service.update(999, new AdminCategoryService.Input("Name", null)));
        verify(categories, never()).saveAndFlush(any()); verify(categories, never()).delete(any());
    }
}
