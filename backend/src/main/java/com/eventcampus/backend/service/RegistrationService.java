package com.eventcampus.backend.service;
import com.eventcampus.backend.dto.EventResponse;
import com.eventcampus.backend.entity.*;
import com.eventcampus.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.List;

@Service
public class RegistrationService {
    private final RegistrationRepository registrations;
    private final EventRepository events;
    private final UserRepository users;
    private final RegistrationWriteLock lock;
    private final QrCodeRepository codes;
    public RegistrationService(RegistrationRepository registrations, EventRepository events, UserRepository users, RegistrationWriteLock lock, QrCodeRepository codes) {
        this.registrations = registrations; this.events = events; this.users = users; this.lock = lock;
        this.codes = codes;
    }
    public record Availability(Integer idEvent, int capacity, long remaining, boolean open) {}
    public record Item(Integer idRegistration, String status, LocalDateTime registeredAt, EventResponse event, boolean cancellable, boolean qrEligible) {}
    private ResponseStatusException error(HttpStatus status, String text) { return new ResponseStatusException(status, text); }
    private LocalDateTime now() { return LocalDateTime.now(ZoneId.of("Europe/Paris")); }
    private boolean future(Event event) { return LocalDateTime.of(event.getEventDate(), event.getEventTime()).isAfter(now()); }
    private User user(String email) {
        var user = users.findByEmail(email).orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Compte introuvable."));
        if (!"ACTIVE".equals(user.getStatus()) || user.getRole() == null || !"USER".equals(user.getRole().getName()))
            throw error(HttpStatus.FORBIDDEN, "Inscription réservée aux étudiants actifs.");
        return user;
    }
    private Event event(Integer id) {
        if (id == null || id <= 0) throw error(HttpStatus.BAD_REQUEST, "Identifiant invalide.");
        return events.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Événement introuvable."));
    }
    private Availability availability(Event event) {
        long used = registrations.countByEvent_IdEventAndStatus(event.getIdEvent(), "REGISTERED");
        return new Availability(event.getIdEvent(), event.getCapacity(), Math.max(0, event.getCapacity() - used), future(event));
    }
    private boolean canViewQr(Registration registration) {
        if (!QrCodeService.eligible(registration)) return false;
        return codes.findByRegistration_IdRegistration(registration.getIdRegistration())
                .map(code -> !code.isUsed() || code.getCode().startsWith("revoked:")).orElse(true);
    }
    @Transactional(readOnly = true)
    public List<Availability> availability() {
        return events.findByStatus("PUBLIE", org.springframework.data.domain.Sort.by("idEvent")).stream()
                .filter(e -> "PUBLIE".equals(e.getStatus())).map(this::availability).toList();
    }
    public void register(String email, Integer id) {
        if (id == null || id <= 0) throw error(HttpStatus.BAD_REQUEST, "Identifiant invalide.");
        lock.execute(id, () -> {
            var user = user(email); var event = event(id);
            if (!"PUBLIE".equals(event.getStatus()) || !future(event)) throw error(HttpStatus.CONFLICT, "Les inscriptions à cet événement sont fermées.");
            var previous = registrations.findByUser_IdUserAndEvent_IdEvent(user.getIdUser(), id).orElse(null);
            if (previous != null && !"CANCELLED".equals(previous.getStatus())) throw error(HttpStatus.CONFLICT, "Vous êtes déjà inscrit à cet événement.");
            if (availability(event).remaining() == 0) throw error(HttpStatus.CONFLICT, "Cet événement est complet.");
            var registration = previous == null ? new Registration() : previous;
            // Revoke before reactivation: on MyISAM a later failed write cannot revive an old token.
            if (previous != null) codes.revoke(previous.getIdRegistration());
            registration.setUser(user); registration.setEvent(event); registration.setStatus("REGISTERED"); registration.setRegisteredAt(now());
            registrations.saveAndFlush(registration); return null;
        });
    }
    @Transactional(readOnly = true)
    public List<Item> mine(String email) {
        return registrations.findByUser_IdUserOrderByRegisteredAtDesc(user(email).getIdUser()).stream()
                .map(r -> new Item(r.getIdRegistration(), r.getStatus(), r.getRegisteredAt(), EventResponse.from(r.getEvent()),
                        "REGISTERED".equals(r.getStatus()) && future(r.getEvent()), canViewQr(r))).toList();
    }
    public void cancel(String email, Integer id) {
        if (id == null || id <= 0) throw error(HttpStatus.BAD_REQUEST, "Identifiant invalide.");
        // First lookup supplies only the lock key. Ownership and state are rechecked under the lock.
        var initial = registrations.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Inscription introuvable."));
        lock.execute(initial.getEvent().getIdEvent(), () -> {
            var user = user(email);
            var registration = registrations.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Inscription introuvable."));
            if (!registration.getUser().getIdUser().equals(user.getIdUser())) throw error(HttpStatus.NOT_FOUND, "Inscription introuvable.");
            if (!"REGISTERED".equals(registration.getStatus()) || !future(registration.getEvent())) throw error(HttpStatus.CONFLICT, "Annulation non autorisée ou déjà effectuée.");
            // Fail safe: revoke first. If cancellation then fails, no old QR becomes valid again.
            codes.revoke(id);
            registration.setStatus("CANCELLED"); registrations.saveAndFlush(registration); return null;
        });
    }
}
