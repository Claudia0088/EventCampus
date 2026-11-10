package com.eventcampus.backend.controller;

import com.eventcampus.backend.dto.ModeratorEventResponse;
import com.eventcampus.backend.service.EventModerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/moderator/events")
public class ModeratorEventController {
    private final EventModerationService moderation;
    public ModeratorEventController(EventModerationService moderation) { this.moderation = moderation; }

    @GetMapping("/stats")
    public EventModerationService.Stats stats() { return moderation.stats(); }

    @GetMapping("/pending")
    public List<ModeratorEventResponse> pending() { return moderation.pending(); }

    @PatchMapping("/{id}/approve")
    public EventModerationService.Decision approve(@PathVariable Integer id) { return moderation.approve(id); }

    @PatchMapping("/{id}/reject")
    public EventModerationService.Decision reject(@PathVariable Integer id) { return moderation.reject(id); }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> error(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason()));
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> invalidId(Exception error) {
        return ResponseEntity.badRequest().body(Map.of("message", "Identifiant invalide."));
    }
}
