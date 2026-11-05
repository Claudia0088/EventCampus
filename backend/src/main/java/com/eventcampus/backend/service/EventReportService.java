package com.eventcampus.backend.service;

import com.eventcampus.backend.dto.EventReportRequest;
import com.eventcampus.backend.entity.*;
import com.eventcampus.backend.repository.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.List;

@Service
public class EventReportService {
    private final EventReportRepository reports;
    private final UserRepository users;
    private final EventRepository events;
    private final EventReportWriteLock lock;
    private final boolean enabled;
    private final Clock clock;

    @Autowired
    public EventReportService(EventReportRepository reports, UserRepository users, EventRepository events,
            EventReportWriteLock lock, @Value("${eventcampus.reports.enabled:false}") boolean enabled) {
        this(reports, users, events, lock, enabled, Clock.system(ZoneId.of("Europe/Paris")));
    }
    public EventReportService(EventReportRepository reports, UserRepository users, EventRepository events,
            EventReportWriteLock lock, boolean enabled, Clock clock) {
        this.reports = reports; this.users = users; this.events = events; this.lock = lock;
        this.enabled = enabled; this.clock = clock.withZone(ZoneId.of("Europe/Paris"));
    }
    public record Item(Integer idReport, Integer idEvent, String eventTitle, String reason, String status, LocalDateTime createdAt) {}
    public record Detail(Item report, String explanation, Integer reporterId, Integer moderatorId, LocalDateTime processedAt) {}
    public record Listing(List<Item> items, int page, int totalPages, long totalElements) {}
    public record Receipt(Integer idReport, String message) {}
    private ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
    private void available() {
        if (!enabled) throw error(HttpStatus.SERVICE_UNAVAILABLE, "Les signalements ne sont pas encore activés. Réessayez après leur activation par l'administrateur.");
    }
    private User actor(String email, String role) {
        var user = users.findByEmail(email).orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Session expirée. Reconnectez-vous."));
        if (!"ACTIVE".equals(user.getStatus()) || user.getRole() == null || !role.equals(user.getRole().getName()))
            throw error(HttpStatus.FORBIDDEN, "Votre compte ne permet pas cette action.");
        return user;
    }
    private Item item(EventReport report) {
        return new Item(report.getIdReport(), report.getEvent().getIdEvent(), report.getEvent().getTitle(),
                report.getReason().name(), report.getStatus().name(), report.getCreatedAt());
    }
    public Receipt create(String email, Integer eventId, EventReportRequest input) {
        available();
        var initial = actor(email, "USER");
        if (eventId == null || eventId <= 0 || input == null) throw error(HttpStatus.BAD_REQUEST, "Événement et motif requis.");
        EventReport.Reason reason;
        try { reason = EventReport.Reason.valueOf(input.reason() == null ? "" : input.reason()); }
        catch (IllegalArgumentException exception) { throw error(HttpStatus.BAD_REQUEST, "Motif de signalement invalide."); }
        if (input.explanation() != null && input.explanation().length() > 2000)
            throw error(HttpStatus.BAD_REQUEST, "L'explication est limitée à 2 000 caractères.");
        String explanation = input.explanation() == null ? "" : input.explanation().strip();
        if (reason == EventReport.Reason.OTHER && explanation.isBlank())
            throw error(HttpStatus.BAD_REQUEST, "Précisez votre motif dans l'explication.");
        return lock.execute(initial.getIdUser(), () -> {
            var reporter = actor(email, "USER");
            var event = events.findById(eventId).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Événement introuvable."));
            if (!"PUBLIE".equals(event.getStatus())) throw error(HttpStatus.CONFLICT, "Cet événement n'est plus disponible pour un signalement.");
            if (reports.existsByReporter_IdUserAndEvent_IdEvent(reporter.getIdUser(), eventId))
                throw error(HttpStatus.CONFLICT, "Vous avez déjà signalé cet événement. Votre signalement a bien été pris en compte.");
            LocalDateTime now = LocalDateTime.now(clock);
            if (reports.countByReporter_IdUserAndCreatedAtGreaterThanEqual(reporter.getIdUser(), now.minusHours(1)) >= 5)
                throw error(HttpStatus.TOO_MANY_REQUESTS, "Limite de 5 signalements par heure atteinte. Réessayez plus tard.");
            var report = new EventReport(); report.setEvent(event); report.setReporter(reporter); report.setReason(reason);
            report.setExplanation(explanation); report.setStatus(EventReport.Status.PENDING); report.setCreatedAt(now);
            report = reports.saveAndFlush(report);
            return new Receipt(report.getIdReport(), "Merci, votre signalement a été transmis à l'équipe de modération.");
        });
    }
    @Transactional(readOnly = true)
    public Listing list(String email, String status, int page) {
        available(); actor(email, "MODERATOR");
        if (page < 0 || page > 100000) throw error(HttpStatus.BAD_REQUEST, "Page invalide.");
        var pageable = PageRequest.of(page, 12, Sort.by(Sort.Direction.DESC, "createdAt", "idReport"));
        Page<EventReport> results;
        if (status == null || status.isBlank()) results = reports.findAll(pageable);
        else {
            EventReport.Status selected;
            try { selected = EventReport.Status.valueOf(status); }
            catch (IllegalArgumentException exception) { throw error(HttpStatus.BAD_REQUEST, "Statut invalide."); }
            results = reports.findByStatus(selected, pageable);
        }
        return new Listing(results.getContent().stream().map(this::item).toList(), page, results.getTotalPages(), results.getTotalElements());
    }
    @Transactional(readOnly = true)
    public Detail detail(String email, Integer id) {
        available(); actor(email, "MODERATOR");
        var report = existing(id);
        return new Detail(item(report), report.getExplanation(), report.getReporter().getIdUser(),
                report.getModerator() == null ? null : report.getModerator().getIdUser(), report.getProcessedAt());
    }
    private EventReport existing(Integer id) {
        if (id == null || id <= 0) throw error(HttpStatus.BAD_REQUEST, "Identifiant de signalement invalide.");
        return reports.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Signalement introuvable."));
    }
    @Transactional
    public Receipt decide(String email, Integer id, String status) {
        available(); var moderator = actor(email, "MODERATOR");
        if (!"RESOLVED".equals(status) && !"REJECTED".equals(status)) throw error(HttpStatus.BAD_REQUEST, "Décision invalide.");
        existing(id);
        if (reports.decidePending(id, status, moderator.getIdUser(), LocalDateTime.now(clock)) != 1)
            throw error(HttpStatus.CONFLICT, "Ce signalement a déjà été traité. Actualisez la liste.");
        return new Receipt(id, "RESOLVED".equals(status) ? "Signalement marqué comme traité." : "Signalement rejeté.");
    }
}
