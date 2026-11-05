package com.eventcampus.backend.service;

import com.eventcampus.backend.dto.ModeratorEventResponse;
import com.eventcampus.backend.repository.EventRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
public class EventModerationService {
    private final EventRepository events;

    public EventModerationService(EventRepository events) { this.events = events; }

    public record Stats(long pending, long published, long rejected, long total) {}

    @Transactional(readOnly = true)
    public Stats stats() {
        var counts = events.getEventStats();
        return new Stats(counts.getPending(), counts.getPublished(), counts.getRejected(), counts.getTotal());
    }

    @Transactional(readOnly = true)
    public List<ModeratorEventResponse> pending() {
        return events.findAllByStatus("EN_ATTENTE", Sort.by("createdAt", "idEvent"))
                .stream().filter(event -> "EN_ATTENTE".equals(event.getStatus()))
                .map(ModeratorEventResponse::from).toList();
    }

    public record Decision(Integer idEvent, String status, String message) {}

    @Transactional
    public Decision approve(Integer id) { return decide(id, "PUBLIE"); }

    @Transactional
    public Decision reject(Integer id) { return decide(id, "REFUSE"); }

    private Decision decide(Integer id, String status) {
        if (id <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identifiant invalide.");
        if (events.decidePending(id, status) == 0) {
            if (!events.existsById(id)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Événement introuvable.");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet événement a déjà été traité. Actualise la liste.");
        }
        return new Decision(id, status, "PUBLIE".equals(status)
                ? "Événement accepté et publié." : "Événement refusé.");
    }
}
