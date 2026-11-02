package com.eventcampus.backend.controller;
import com.eventcampus.backend.service.QrCodeService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/user/registrations")
public class QrCodeController {
    private final QrCodeService codes;
    public QrCodeController(QrCodeService codes) { this.codes = codes; }
    @PostMapping(value = "/{id}/qr-code", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> png(Principal principal, @PathVariable Integer id) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache").header("Content-Disposition", "inline; filename=eventcampus-qr-" + id + ".png")
                .body(codes.png(principal.getName(), id));
    }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> error(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).contentType(MediaType.APPLICATION_JSON).cacheControl(CacheControl.noStore()).body(Map.of("message", error.getReason()));
    }
}
