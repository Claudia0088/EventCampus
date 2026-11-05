package com.eventcampus.backend.service;

import com.eventcampus.backend.entity.*;
import com.eventcampus.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.List;

@Service
public class QrCheckInService {
    private final EventRepository events;
    private final UserRepository users;
    private final QrCodeRepository codes;
    private final AttendanceRepository attendances;
    private final RegistrationWriteLock lock;
    private final QrTokenCodec codec;
    private final Clock clock;

    @Autowired
    public QrCheckInService(EventRepository events, UserRepository users, QrCodeRepository codes,
            AttendanceRepository attendances, RegistrationWriteLock lock, QrTokenCodec codec) {
        this(events, users, codes, attendances, lock, codec, Clock.system(ZoneId.of("Europe/Paris")));
    }
    public QrCheckInService(EventRepository events, UserRepository users, QrCodeRepository codes,
            AttendanceRepository attendances, RegistrationWriteLock lock, QrTokenCodec codec, Clock clock) {
        this.events = events; this.users = users; this.codes = codes; this.attendances = attendances;
        this.lock = lock; this.codec = codec; this.clock = clock.withZone(ZoneId.of("Europe/Paris"));
    }
    public record ScanEvent(Integer idEvent, String title, LocalDate eventDate) {}
    public record Result(String code, String message, Integer idRegistration, String student, LocalDateTime scannedAt) {}

    private void authorize(String email) {
        var user = users.findByEmail(email).orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Session absente ou expirée."));
        if (!"ACTIVE".equals(user.getStatus()) || user.getRole() == null
                || !List.of("ADMIN", "MODERATOR").contains(user.getRole().getName()))
            throw error(HttpStatus.FORBIDDEN, "Le scan est réservé aux administrateurs et modérateurs actifs.");
    }
    private ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }

    @Transactional(readOnly = true)
    public List<ScanEvent> today(String email) {
        authorize(email);
        return events.findByStatus("PUBLIE", Sort.by("eventTime", "idEvent")).stream()
                .filter(event -> "PUBLIE".equals(event.getStatus()) && LocalDate.now(clock).equals(event.getEventDate()))
                .map(event -> new ScanEvent(event.getIdEvent(), event.getTitle(), event.getEventDate())).toList();
    }

    public Result scan(String email, Integer eventId, String token) {
        if (eventId == null || eventId <= 0) throw error(HttpStatus.BAD_REQUEST, "Choisissez un événement valide.");
        String fingerprint = codec.fingerprint(token);
        return lock.execute(eventId, () -> {
            authorize(email);
            var event = events.findById(eventId).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Événement introuvable."));
            if (!"PUBLIE".equals(event.getStatus())) throw error(HttpStatus.GONE, "Événement indisponible.");
            if (event.getEventDate().isBefore(LocalDate.now(clock))) throw error(HttpStatus.GONE, "Billet expiré : événement terminé.");
            if (!event.getEventDate().equals(LocalDate.now(clock))) throw error(HttpStatus.CONFLICT, "La présence peut être validée uniquement le jour de l'événement (heure de Paris).");
            // Keep existing EC1 tickets: the stored envelope ends with SHA-256(token).
            var matches = codes.findByRegistration_Event_IdEventAndCodeEndingWith(eventId, "." + fingerprint);
            if (matches.size() != 1) throw error(HttpStatus.BAD_REQUEST, "QR code invalide ou ne correspondant pas à cet événement.");
            var code = matches.getFirst();
            var registration = code.getRegistration();
            if (!eventId.equals(registration.getEvent().getIdEvent())
                    || !codec.matches(registration.getIdRegistration(), code.getCode(), token))
                throw error(HttpStatus.BAD_REQUEST, "QR code invalide.");
            var student = registration.getUser();
            if (!"REGISTERED".equals(registration.getStatus()) || !"ACTIVE".equals(student.getStatus())
                    || student.getRole() == null || !"USER".equals(student.getRole().getName()))
                throw error(HttpStatus.GONE, "Inscription annulée ou compte étudiant indisponible.");

            var attendance = attendances.findByRegistration_IdRegistration(registration.getIdRegistration()).orElse(null);
            boolean already = code.isUsed() || (attendance != null && attendance.isPresent());
            LocalDateTime scannedAt = attendance != null && attendance.isPresent() && attendance.getScannedAt() != null
                    ? attendance.getScannedAt() : code.isUsed() && code.getUsedAt() != null ? code.getUsedAt() : LocalDateTime.now(clock);
            // Attendance first. On MyISAM, a failed second write is repaired by the next scan,
            // preserving the original timestamp. The shared event lock serializes scan/cancel.
            if (attendance == null || !attendance.isPresent()) {
                if (attendance == null) attendance = new Attendance();
                attendance.setRegistration(registration); attendance.setPresent(true); attendance.setScannedAt(scannedAt);
                attendances.saveAndFlush(attendance);
            }
            if (!code.isUsed()) {
                code.setUsed(true); code.setUsedAt(scannedAt); codes.saveAndFlush(code);
            }
            return new Result(already ? "ALREADY_USED" : "VALID",
                    already ? "Présence déjà enregistrée. Ce billet a déjà été utilisé." : "QR code valide. Présence enregistrée.",
                    registration.getIdRegistration(), student.getFirstName() + " " + student.getLastName(), scannedAt);
        });
    }
}
