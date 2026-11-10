package com.eventcampus.backend.service;

import com.eventcampus.backend.dto.EventProposalRequest;
import com.eventcampus.backend.dto.EventResponse;
import com.eventcampus.backend.entity.Event;
import com.eventcampus.backend.entity.User;
import com.eventcampus.backend.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class EventProposalService {
    private final EventRepository events;
    private final UserRepository users;
    private final CategoryRepository categories;
    private final EntityManager entityManager;
    private final CategoryWriteLock categoryLock;

    public EventProposalService(EventRepository events, UserRepository users,
            CategoryRepository categories, EntityManager entityManager, CategoryWriteLock categoryLock) {
        this.events = events;
        this.users = users;
        this.categories = categories;
        this.entityManager = entityManager;
        this.categoryLock = categoryLock;
    }

    private User creator(String email) {
        var user = users.findByEmail(email).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Compte introuvable."));
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Compte inactif.");
        }
        return user;
    }

    public EventResponse propose(String email, EventProposalRequest request) {
        return categoryLock.execute(() -> proposeLocked(email, request));
    }

    private EventResponse proposeLocked(String email, EventProposalRequest request) {
        var user = creator(email);
        var category = categories.findById(request.idCategory()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Catégorie inconnue."));
        if (!LocalDateTime.of(request.eventDate(), request.eventTime())
                .isAfter(LocalDateTime.now(ZoneId.of("Europe/Paris")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La date et l'heure doivent être futures.");
        }
        var event = new Event();
        event.setTitle(request.title().trim());
        event.setDescription(request.description().trim());
        event.setEventDate(request.eventDate());
        event.setEventTime(request.eventTime().withNano(0));
        event.setLocation(request.location().trim());
        event.setCapacity(request.capacity());
        event.setCategory(category);
        event.setCreator(user);
        event.setStatus("EN_ATTENTE");
        events.saveAndFlush(event);
        // MySQL generates the creation/update timestamps; reload them for the response.
        entityManager.refresh(event);
        return EventResponse.from(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponse> mine(String email) {
        return events.findByCreator_IdUser(creator(email).getIdUser(),
                Sort.by(Sort.Direction.DESC, "createdAt", "idEvent"))
                .stream().map(EventResponse::from).toList();
    }
}
