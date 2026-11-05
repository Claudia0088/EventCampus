package com.eventcampus.backend.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {UserEventReportController.class, ModeratorReportController.class})
public class EventReportErrors {
    private ResponseEntity<?> response(HttpStatusCode status, String message) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(Map.of("message", message));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException error) {
        return response(error.getStatusCode(), error.getReason());
    }
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class})
    public ResponseEntity<?> invalid() {
        return response(HttpStatus.BAD_REQUEST, "Données invalides. Vérifiez le motif, la décision et l'explication (2 000 caractères maximum).");
    }
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<?> duplicate() {
        return response(HttpStatus.CONFLICT, "Ce signalement existe déjà ou ne peut plus être enregistré. Actualisez la page.");
    }
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<?> databaseUnavailable() {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "Les signalements sont temporairement indisponibles. Réessayez plus tard.");
    }
}
