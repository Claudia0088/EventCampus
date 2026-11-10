package com.eventcampus.backend.controller;

import com.eventcampus.backend.service.QrCheckInService;
import com.eventcampus.backend.service.QrImageDecoder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/check-in")
public class QrCheckInController {
    private final QrCheckInService service;
    public QrCheckInController(QrCheckInService service) { this.service = service; }
    public record ScanRequest(@NotNull @Positive Integer eventId, @NotBlank @Size(max = 47) String token) {}

    @GetMapping("/events")
    public ResponseEntity<?> today(Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.today(principal.getName()));
    }
    // Custom header + JSON prevent cross-origin simple form submissions. CORS stays restricted.
    private void requireIntent(String header) {
        if (!"1".equals(header)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Requête de scan invalide.");
    }
    @PostMapping(value = "/scan", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> scan(Principal principal, @RequestHeader(value = "X-EventCampus-Scan", defaultValue = "") String intent,
            @Valid @RequestBody ScanRequest request) {
        requireIntent(intent);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.scan(principal.getName(), request.eventId(), request.token()));
    }
    @PostMapping(value = "/scan-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> image(Principal principal, @RequestHeader(value = "X-EventCampus-Scan", defaultValue = "") String intent,
            @RequestParam Integer eventId, @RequestParam MultipartFile image) throws java.io.IOException {
        requireIntent(intent);
        if (image.getSize() > 5 * 1024 * 1024) throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Image trop volumineuse (5 Mo maximum).");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.scan(principal.getName(), eventId, QrImageDecoder.decode(image.getBytes())));
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> error(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).cacheControl(CacheControl.noStore())
                .body(Map.of("message", error.getReason()));
    }
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<?> invalid() {
        return ResponseEntity.badRequest().cacheControl(CacheControl.noStore()).body(Map.of("message", "Événement, image ou QR code invalide."));
    }
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<?> unavailable() {
        return ResponseEntity.status(503).cacheControl(CacheControl.noStore())
                .body(Map.of("message", "Enregistrement indisponible. Réessayez le même billet ; aucune présence ne sera comptée deux fois."));
    }
}
