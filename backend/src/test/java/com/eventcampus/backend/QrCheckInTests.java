package com.eventcampus.backend;

import com.eventcampus.backend.entity.*;
import com.eventcampus.backend.repository.*;
import com.eventcampus.backend.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Pure unit tests. No Spring context, database, network or real key file.
class QrCheckInTests {
    @TempDir Path temporary;
    EventRepository events = mock(EventRepository.class);
    UserRepository users = mock(UserRepository.class);
    QrCodeRepository codes = mock(QrCodeRepository.class);
    AttendanceRepository attendances = mock(AttendanceRepository.class);
    RegistrationWriteLock lock = mock(RegistrationWriteLock.class);
    Clock clock = Clock.fixed(Instant.parse("2026-11-05T12:00:00Z"), ZoneId.of("Europe/Paris"));
    QrTokenCodec codec; QrCheckInService service;
    Event event; Registration registration; User staff, student; QrCode code; Attendance attendance;
    String token;

    @BeforeEach void setup() throws Exception {
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        Path key = temporary.resolve("qr.key"); Files.write(key, bytes); Arrays.fill(bytes, (byte)0);
        codec = new QrTokenCodec(key);
        service = new QrCheckInService(events, users, codes, attendances, lock, codec, clock);
        var moderatorRole = new Role(); moderatorRole.setName("MODERATOR");
        staff = new User(); staff.setStatus("ACTIVE"); staff.setRole(moderatorRole);
        var userRole = new Role(); userRole.setName("USER");
        student = new User(); student.setIdUser(1); student.setStatus("ACTIVE"); student.setRole(userRole); student.setFirstName("Camille"); student.setLastName("Demo");
        event = new Event(); event.setIdEvent(10); event.setStatus("PUBLIE"); event.setTitle("Demo"); event.setEventDate(LocalDate.now(clock));
        registration = new Registration(); registration.setIdRegistration(20); registration.setUser(student); registration.setEvent(event); registration.setStatus("REGISTERED");
        code = new QrCode(); code.setRegistration(registration); code.setCode(codec.issue(20)); token = codec.recover(20, code.getCode());
        when(users.findByEmail("staff@test")).thenReturn(Optional.of(staff));
        when(events.findById(10)).thenReturn(Optional.of(event));
        when(codes.findByRegistration_Event_IdEventAndCodeEndingWith(10,"." + codec.fingerprint(token))).thenReturn(List.of(code));
        when(attendances.findByRegistration_IdRegistration(20)).thenAnswer(call -> Optional.ofNullable(attendance));
        when(attendances.saveAndFlush(any())).thenAnswer(call -> { attendance = call.getArgument(0); return attendance; });
        when(codes.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        Object monitor = new Object();
        when(lock.execute(eq(10),any())).thenAnswer(call -> { synchronized (monitor) { return ((Supplier<?>)call.getArgument(1)).get(); } });
    }
    void status(HttpStatus expected, Runnable work) {
        assertEquals(expected, assertThrows(ResponseStatusException.class,work::run).getStatusCode());
        verify(attendances,never()).saveAndFlush(any());
    }
    @Test void validTicketRecordsPresenceOnceAndRepeatDoesNotChangeTimestamp() {
        var first = service.scan("staff@test",10,token);
        var second = service.scan("staff@test",10,token);
        assertEquals("VALID", first.code()); assertEquals("ALREADY_USED",second.code());
        assertEquals(first.scannedAt(),second.scannedAt()); assertTrue(attendance.isPresent()); assertTrue(code.isUsed());
        verify(attendances,times(1)).saveAndFlush(any()); verify(codes,times(1)).saveAndFlush(any());
    }
    @Test void simultaneousScansCreateOnlyOneAttendance() throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        try {
            var one = executor.submit(() -> service.scan("staff@test",10,token));
            var two = executor.submit(() -> service.scan("staff@test",10,token));
            assertEquals(Set.of("VALID","ALREADY_USED"), Set.of(one.get(5,TimeUnit.SECONDS).code(),two.get(5,TimeUnit.SECONDS).code()));
            verify(attendances,times(1)).saveAndFlush(any());
        } finally { executor.shutdownNow(); }
    }
    @Test void repairsMyisamPartialWriteWithoutCountingAgain() {
        when(codes.saveAndFlush(any())).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("simulated"));
        assertThrows(org.springframework.dao.DataAccessException.class, () -> service.scan("staff@test",10,token));
        LocalDateTime original = attendance.getScannedAt();
        // Model persisted MyISAM attendance and an unpersisted QR flag after the failure.
        code.setUsed(false); code.setUsedAt(null);
        doAnswer(call -> call.getArgument(0)).when(codes).saveAndFlush(any());
        var result = service.scan("staff@test",10,token);
        assertEquals("ALREADY_USED",result.code()); assertEquals(original,result.scannedAt()); assertTrue(code.isUsed());
        verify(attendances,times(1)).saveAndFlush(any());
    }
    @Test void existingUnusedAttendanceRowIsUpdatedRatherThanDuplicated() {
        attendance = new Attendance(); attendance.setRegistration(registration); attendance.setPresent(false);
        var original = attendance;
        assertEquals("VALID",service.scan("staff@test",10,token).code());
        verify(attendances).saveAndFlush(same(original));
    }
    @Test void consumedQrWithMissingAttendanceIsReconciled() {
        code.setUsed(true); code.setUsedAt(LocalDateTime.now(clock).minusMinutes(5));
        assertEquals("ALREADY_USED",service.scan("staff@test",10,token).code());
        assertEquals(code.getUsedAt(),attendance.getScannedAt());
    }
    @Test void adminIsAlsoAuthorized() {
        staff.getRole().setName("ADMIN");
        assertEquals("VALID",service.scan("staff@test",10,token).code());
    }
    @Test void studentCannotScan() {
        staff.getRole().setName("USER"); status(HttpStatus.FORBIDDEN, () -> service.scan("staff@test",10,token));
    }
    @Test void inactiveStaffCannotScan() {
        staff.setStatus("INACTIVE"); status(HttpStatus.FORBIDDEN, () -> service.scan("staff@test",10,token));
    }
    @Test void missingStaffIsUnauthorized() {
        status(HttpStatus.UNAUTHORIZED, () -> service.scan("missing@test",10,token));
    }
    @Test void expiredAndFutureEventsCannotBeValidated() {
        event.setEventDate(LocalDate.now(clock).minusDays(1)); status(HttpStatus.GONE, () -> service.scan("staff@test",10,token));
        event.setEventDate(LocalDate.now(clock).plusDays(1)); status(HttpStatus.CONFLICT, () -> service.scan("staff@test",10,token));
    }
    @Test void dateIsParisEvenAfterUtcDayBoundary() {
        var lateClock = Clock.fixed(Instant.parse("2026-11-05T23:30:00Z"),ZoneOffset.UTC);
        var lateService = new QrCheckInService(events,users,codes,attendances,lock,codec,lateClock);
        status(HttpStatus.GONE, () -> lateService.scan("staff@test",10,token));
    }
    @Test void unpublishedAndMissingEventsAreRejected() {
        event.setStatus("REFUSE"); status(HttpStatus.GONE, () -> service.scan("staff@test",10,token));
        when(events.findById(10)).thenReturn(Optional.empty()); status(HttpStatus.NOT_FOUND, () -> service.scan("staff@test",10,token));
    }
    @Test void cancelledAndInactiveStudentAreRejected() {
        registration.setStatus("CANCELLED"); status(HttpStatus.GONE, () -> service.scan("staff@test",10,token));
        registration.setStatus("REGISTERED"); student.setStatus("INACTIVE"); status(HttpStatus.GONE, () -> service.scan("staff@test",10,token));
    }
    @Test void wrongEventAndInvalidTokenAreRejected() {
        when(codes.findByRegistration_Event_IdEventAndCodeEndingWith(anyInt(),anyString())).thenReturn(List.of());
        status(HttpStatus.BAD_REQUEST, () -> service.scan("staff@test",10,token));
        status(HttpStatus.BAD_REQUEST, () -> service.scan("staff@test",10,"https://untrusted.example"));
        status(HttpStatus.BAD_REQUEST, () -> service.scan("staff@test",0,token));
    }
    @Test void hashAloneIsInsufficientWithoutAuthenticHmac() {
        code.setCode("v1." + "A".repeat(32) + "." + codec.fingerprint(token));
        status(HttpStatus.BAD_REQUEST, () -> service.scan("staff@test",10,token));
    }
    @Test void missingKeyPreventsAttendance() {
        var failing = new QrCheckInService(events,users,codes,attendances,lock,new QrTokenCodec(temporary.resolve("missing")),clock);
        status(HttpStatus.SERVICE_UNAVAILABLE, () -> failing.scan("staff@test",10,token));
    }
    @Test void todayListsOnlyPublishedEventsForParisDate() {
        var tomorrow = new Event(); tomorrow.setEventDate(LocalDate.now(clock).plusDays(1)); tomorrow.setStatus("PUBLIE");
        when(events.findByStatus(eq("PUBLIE"),any())).thenReturn(List.of(event,tomorrow));
        assertEquals(List.of(new QrCheckInService.ScanEvent(10,"Demo",LocalDate.now(clock))),service.today("staff@test"));
    }
    @Test void imagesRoundTripAndBadImagesAreRejected() {
        assertEquals(token,QrImageDecoder.decode(QrCodeService.render(token)));
        status(HttpStatus.BAD_REQUEST, () -> QrImageDecoder.decode(new byte[]{1,2,3}));
        status(HttpStatus.BAD_REQUEST, () -> QrImageDecoder.decode(new byte[5 * 1024 * 1024 + 1]));
    }
    @Test void oversizedImageDimensionsAreRejectedBeforeDecode() throws Exception {
        var image = new java.awt.image.BufferedImage(4097,1,java.awt.image.BufferedImage.TYPE_INT_RGB);
        var output = new java.io.ByteArrayOutputStream(); javax.imageio.ImageIO.write(image,"png",output);
        status(HttpStatus.BAD_REQUEST, () -> QrImageDecoder.decode(output.toByteArray()));
    }
}
