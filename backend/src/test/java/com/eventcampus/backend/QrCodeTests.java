package com.eventcampus.backend;
import com.eventcampus.backend.entity.*;
import com.eventcampus.backend.repository.*;
import com.eventcampus.backend.service.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QrCodeTests {
    @TempDir Path temporary;
    RegistrationRepository registrations = mock(RegistrationRepository.class);
    QrCodeRepository codes = mock(QrCodeRepository.class);
    UserRepository users = mock(UserRepository.class);
    RegistrationWriteLock lock = mock(RegistrationWriteLock.class);
    QrTokenCodec codec; QrCodeService service;
    Registration registration; User user; QrCode stored;
    @BeforeEach void setup() throws Exception {
        byte[] key = new byte[32]; new java.security.SecureRandom().nextBytes(key); Path file = temporary.resolve("key"); Files.write(file,key);
        codec = new QrTokenCodec(file); service = new QrCodeService(registrations,codes,users,lock,codec);
        var role = new Role(); role.setName("USER");
        user = new User(); user.setIdUser(1); user.setRole(role); user.setStatus("ACTIVE");
        var event = new Event(); event.setIdEvent(10); event.setStatus("PUBLIE"); event.setEventDate(LocalDate.now().plusDays(2));
        registration = new Registration(); registration.setIdRegistration(20); registration.setUser(user); registration.setEvent(event); registration.setStatus("REGISTERED");
        when(registrations.findById(20)).thenReturn(Optional.of(registration)); when(users.findByEmail("student@test")).thenReturn(Optional.of(user));
        when(codes.findByRegistration_IdRegistration(20)).thenAnswer(call -> Optional.ofNullable(stored));
        when(codes.saveAndFlush(any())).thenAnswer(call -> { stored=call.getArgument(0); return stored; });
        Object monitor = new Object(); when(lock.execute(eq(10),any())).thenAnswer(call -> { synchronized(monitor) { return ((Supplier<?>)call.getArgument(1)).get(); } });
    }
    String decode(byte[] png) throws Exception {
        var image=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        var source=new RGBLuminanceSource(image.getWidth(),image.getHeight(),image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth()));
        return new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(source))).getText();
    }
    @Test void pngDecodesAndStoredValueIsOnlyNonceAndHash() throws Exception {
        String token=decode(service.png("student@test",20));
        assertTrue(token.matches("EC1\\.[A-Za-z0-9_-]{43}")); assertFalse(stored.getCode().contains(token)); assertTrue(stored.getCode().length()<=191);
        assertTrue(service.valid(registration,stored,token)); assertEquals(token,decode(service.png("student@test",20)));
        verify(codes,times(1)).saveAndFlush(any());
    }
    @Test void concurrentRequestsReturnSameTicketWithoutDuplicate() throws Exception {
        var pool=Executors.newFixedThreadPool(2);
        try {
            var first=pool.submit(()->service.png("student@test",20)); var second=pool.submit(()->service.png("student@test",20));
            assertEquals(decode(first.get(5,TimeUnit.SECONDS)),decode(second.get(5,TimeUnit.SECONDS))); verify(codes,times(1)).saveAndFlush(any());
        } finally {pool.shutdownNow();}
    }
    @Test void cancellationAndReenrollmentNeverReuseOldToken() throws Exception {
        String old=decode(service.png("student@test",20));
        stored.setCode("revoked:20"); registration.setStatus("CANCELLED");
        assertFalse(service.valid(registration,stored,old)); assertThrows(ResponseStatusException.class,()->service.png("student@test",20));
        registration.setStatus("REGISTERED"); String fresh=decode(service.png("student@test",20));
        assertNotEquals(old,fresh); assertFalse(service.valid(registration,stored,old)); assertTrue(service.valid(registration,stored,fresh));
    }
    @Test void ownerActiveStatusExpiryAndConsumptionAreChecked() {
        var other=new User(); other.setIdUser(2); other.setRole(user.getRole()); other.setStatus("ACTIVE"); when(users.findByEmail("other@test")).thenReturn(Optional.of(other));
        assertThrows(ResponseStatusException.class,()->service.png("other@test",20));
        user.setStatus("INACTIVE"); assertThrows(ResponseStatusException.class,()->service.png("student@test",20)); user.setStatus("ACTIVE");
        registration.getEvent().setEventDate(LocalDate.now().minusDays(1)); assertThrows(ResponseStatusException.class,()->service.png("student@test",20));
        registration.getEvent().setEventDate(LocalDate.now().plusDays(1)); service.png("student@test",20); stored.setUsed(true);
        assertThrows(ResponseStatusException.class,()->service.png("student@test",20));
    }
    @Test void missingKeyFailsWithoutCreatingQrRow() {
        var failing=new QrCodeService(registrations,codes,users,lock,new QrTokenCodec(temporary.resolve("missing")));
        assertThrows(ResponseStatusException.class,()->failing.png("student@test",20)); verify(codes,never()).saveAndFlush(any());
    }
}
