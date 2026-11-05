package com.eventcampus.backend.controller;

import com.eventcampus.backend.dto.*;
import com.eventcampus.backend.repository.CategoryRepository;
import com.eventcampus.backend.service.EventProposalService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/events")
public class UserEventController {
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<?> businessError(org.springframework.web.server.ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason()));
    }

    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<?> invalidRequest(Exception error) {
        return ResponseEntity.badRequest().body(Map.of("message",
                "Données invalides. Vérifie les champs, leurs longueurs, la capacité et la date."));
    }
    private final EventProposalService proposals;
    private final CategoryRepository categories;

    public UserEventController(EventProposalService proposals, CategoryRepository categories) {
        this.proposals = proposals;
        this.categories = categories;
    }

    @PostMapping
    public ResponseEntity<?> propose(Principal principal, @Valid @RequestBody EventProposalRequest request) {
        var event = proposals.propose(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Proposition envoyée. Elle est en attente de validation.", "event", event));
    }

    @GetMapping("/mine")
    public List<EventResponse> mine(Principal principal) {
        return proposals.mine(principal.getName());
    }

    public record CategoryOption(Integer idCategory, String name) {}

    @GetMapping("/categories")
    public List<CategoryOption> categories() {
        return categories.findAll(Sort.by("name")).stream()
                .map(c -> new CategoryOption(c.getIdCategory(), c.getName())).toList();
    }
}
