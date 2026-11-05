package com.eventcampus.backend.controller;

import com.eventcampus.backend.service.EventReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;

@RestController
@RequestMapping("/api/moderator/reports")
public class ModeratorReportController {
    private final EventReportService reports;
    public ModeratorReportController(EventReportService reports) { this.reports = reports; }
    public record Decision(@NotBlank @Pattern(regexp = "RESOLVED|REJECTED") String status) {}
    @GetMapping
    public ResponseEntity<?> list(Principal principal, @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reports.list(principal.getName(), status, page));
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> detail(Principal principal, @PathVariable Integer id) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reports.detail(principal.getName(), id));
    }
    @PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> decide(Principal principal, @PathVariable Integer id,
            @RequestHeader(value = "X-EventCampus-Report", defaultValue = "") String intent,
            @Valid @RequestBody Decision decision) {
        if (!"1".equals(intent)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requête de traitement invalide.");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(reports.decide(principal.getName(), id, decision.status()));
    }
}
