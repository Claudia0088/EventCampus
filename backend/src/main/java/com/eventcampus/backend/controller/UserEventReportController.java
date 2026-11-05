package com.eventcampus.backend.controller;

import com.eventcampus.backend.dto.EventReportRequest;
import com.eventcampus.backend.service.EventReportService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;

@RestController
@RequestMapping("/api/user/events")
public class UserEventReportController {
    private final EventReportService reports;
    public UserEventReportController(EventReportService reports) { this.reports = reports; }
    @PostMapping(value = "/{id}/reports", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(Principal principal, @PathVariable Integer id,
            @RequestHeader(value = "X-EventCampus-Report", defaultValue = "") String intent,
            @Valid @RequestBody EventReportRequest input) {
        if (!"1".equals(intent)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requête de signalement invalide.");
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(reports.create(principal.getName(), id, input));
    }
}
