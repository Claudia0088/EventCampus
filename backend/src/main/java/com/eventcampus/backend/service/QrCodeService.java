package com.eventcampus.backend.service;
import com.eventcampus.backend.entity.*;
import com.eventcampus.backend.repository.*;
import com.google.zxing.*;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.util.Map;

@Service
public class QrCodeService {
    private final RegistrationRepository registrations;
    private final QrCodeRepository codes;
    private final UserRepository users;
    private final RegistrationWriteLock lock;
    private final QrTokenCodec codec;
    public QrCodeService(RegistrationRepository registrations, QrCodeRepository codes, UserRepository users, RegistrationWriteLock lock, QrTokenCodec codec) {
        this.registrations = registrations; this.codes = codes; this.users = users; this.lock = lock; this.codec = codec;
    }
    public static boolean eligible(Registration registration) {
        return "REGISTERED".equals(registration.getStatus()) && "PUBLIE".equals(registration.getEvent().getStatus())
                && !registration.getEvent().getEventDate().isBefore(LocalDate.now(ZoneId.of("Europe/Paris")));
    }
    private ResponseStatusException error(HttpStatus status, String text) { return new ResponseStatusException(status, text); }
    private Registration find(Integer id) {
        if (id == null || id <= 0) throw error(HttpStatus.BAD_REQUEST, "Identifiant invalide.");
        return registrations.findById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Inscription introuvable."));
    }
    public byte[] png(String email, Integer id) {
        var initial = find(id);
        return lock.execute(initial.getEvent().getIdEvent(), () -> {
            var registration = find(id);
            var user = users.findByEmail(email).orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "Compte introuvable."));
            if (!"ACTIVE".equals(user.getStatus()) || user.getRole() == null || !"USER".equals(user.getRole().getName())) throw error(HttpStatus.FORBIDDEN, "Compte étudiant actif requis.");
            if (!user.getIdUser().equals(registration.getUser().getIdUser())) throw error(HttpStatus.NOT_FOUND, "Inscription introuvable.");
            if (!eligible(registration)) throw error(HttpStatus.GONE, "Inscription annulée, événement indisponible ou billet expiré.");
            var code = codes.findByRegistration_IdRegistration(id).orElse(null);
            if (code != null && code.isUsed() && !code.getCode().startsWith("revoked:")) throw error(HttpStatus.GONE, "Ce billet a déjà été utilisé.");
            boolean issue = code == null || code.getCode().startsWith("revoked:");
            String envelope = issue ? codec.issue(id) : code.getCode();
            byte[] image = render(codec.recover(id, envelope));
            if (issue) {
                if (code == null) code = new QrCode();
                code.setRegistration(registration); code.setCode(envelope); code.setUsed(false); code.setUsedAt(null);
                code.setCreatedAt(LocalDateTime.now(ZoneId.of("Europe/Paris"))); codes.saveAndFlush(code);
            }
            return image;
        });
    }
    public static byte[] render(String token) {
        try {
            var matrix = new QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, 360, 360,
                    Map.of(EncodeHintType.MARGIN, 4, EncodeHintType.ERROR_CORRECTION, com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M));
            var image = new BufferedImage(360, 360, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 360; y++) for (int x = 0; x < 360; x++) image.setRGB(x, y, matrix.get(x,y) ? 0x1e293b : 0xffffff);
            var output = new ByteArrayOutputStream(); ImageIO.write(image, "png", output); return output.toByteArray();
        } catch (Exception error) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Génération du QR code indisponible."); }
    }
    // Prepared for a future authorized scanner. No public verification or attendance endpoint exists.
    public boolean valid(Registration registration, QrCode code, String token) {
        return eligible(registration) && "ACTIVE".equals(registration.getUser().getStatus()) && !code.isUsed()
                && codec.matches(registration.getIdRegistration(), code.getCode(), token);
    }
}
