package com.eventcampus.backend.service;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

@Component
public class QrTokenCodec {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(QrTokenCodec.class);
    private final Path keyFile;
    public QrTokenCodec() {
        String configured = System.getenv("EVENTCAMPUS_QR_KEY_FILE");
        String local = System.getenv("LOCALAPPDATA");
        keyFile = configured != null ? Path.of(configured) : Path.of(local != null ? local : System.getProperty("user.home"), "EventCampus", "Secrets", "qr.key");
    }
    public QrTokenCodec(Path keyFile) { this.keyFile = keyFile; }
    @jakarta.annotation.PostConstruct
    public void checkKeyAccess() {
        try {
            byte[] key = Files.readAllBytes(keyFile);
            try {
                if (key.length != 32) throw new GeneralSecurityException();
                Mac.getInstance("HmacSHA256").init(new SecretKeySpec(key, "HmacSHA256"));
            } finally { Arrays.fill(key, (byte)0); }
            log.info("QR server key readiness: OK");
        } catch (Exception error) {
            // Exception type only: never log a key, token, file contents or exception message.
            log.warn("QR server key readiness failed: {}", error.getClass().getSimpleName());
        }
    }
    private String token(Integer registration, String nonce) {
        try {
            byte[] key = Files.readAllBytes(keyFile);
            if (key.length != 32) throw new GeneralSecurityException();
            var mac = Mac.getInstance("HmacSHA256"); mac.init(new SecretKeySpec(key, "HmacSHA256")); Arrays.fill(key, (byte)0);
            return "EC1." + Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal((registration + ":" + nonce).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            log.warn("QR server key operation failed: {}", error.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Clé QR serveur indisponible. Contactez l'administration.");
        }
    }
    private String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (GeneralSecurityException error) { throw new IllegalStateException("QR cryptography unavailable"); }
    }
    public String issue(Integer registration) {
        byte[] random = new byte[24]; new SecureRandom().nextBytes(random);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        return "v1." + nonce + "." + digest(token(registration, nonce));
    }
    public String recover(Integer registration, String envelope) {
        if (envelope == null || !envelope.matches("v1\\.[A-Za-z0-9_-]{32}\\.[a-f0-9]{64}"))
            throw new ResponseStatusException(HttpStatus.GONE, "QR code invalidé.");
        String result = token(registration, envelope.split("\\.")[1]);
        if (!matches(registration, envelope, result)) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "La clé serveur ne correspond pas au billet existant.");
        return result;
    }
    public boolean matches(Integer registration, String envelope, String candidate) {
        if (candidate == null || envelope == null || !envelope.matches("v1\\.[A-Za-z0-9_-]{32}\\.[a-f0-9]{64}")) return false;
        String[] parts = envelope.split("\\.");
        return MessageDigest.isEqual(parts[2].getBytes(StandardCharsets.US_ASCII), digest(candidate).getBytes(StandardCharsets.US_ASCII))
                && MessageDigest.isEqual(token(registration, parts[1]).getBytes(StandardCharsets.US_ASCII), candidate.getBytes(StandardCharsets.US_ASCII));
    }
}
