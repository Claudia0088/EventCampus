package com.eventcampus.backend.mysqlit;

import com.eventcampus.backend.dto.RegisterRequest;
import com.eventcampus.backend.entity.*;
import com.eventcampus.backend.repository.*;
import com.eventcampus.backend.service.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real persistence scenarios, exclusively on the guarded disposable instance. No rollback assumptions. */
class MysqlPersistenceIT extends IsolatedMysqlTestBase {
    @Autowired RegisterService accounts;
    @Autowired AuthService authentication;
    @Autowired UserRepository users;
    @Autowired EventRepository events;
    @Autowired CategoryRepository categories;
    @Autowired RegistrationRepository registrations;
    @Autowired RegistrationService enrollment;
    @Autowired QrCodeService qr;
    @Autowired QrCheckInService scanner;
    @Autowired AttendanceRepository attendances;
    @Autowired javax.sql.DataSource datasource;

    User student() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Camille"); request.setLastName("Fictif");
        request.setEmail("it-" + UUID.randomUUID() + "@example.invalid");
        request.setPassword(UUID.randomUUID().toString());
        return accounts.register(request);
    }
    Event event(User creator, LocalDate day, int capacity) {
        Event event = new Event(); event.setTitle("Integration fictive " + UUID.randomUUID());
        event.setEventDate(day); event.setEventTime(LocalTime.MAX); event.setLocation("Salle fictive");
        event.setCapacity(capacity); event.setStatus("PUBLIE"); event.setCreator(creator);
        event.setCategory(categories.findById(1).orElseThrow());
        return events.saveAndFlush(event);
    }
    int register(User user, Event event) {
        enrollment.register(user.getEmail(), event.getIdEvent());
        return registrations.findByUser_IdUserAndEvent_IdEvent(user.getIdUser(), event.getIdEvent())
                .orElseThrow().getIdRegistration();
    }
    String token(User user, int registration) throws Exception {
        var image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(qr.png(user.getEmail(), registration)));
        var source = new RGBLuminanceSource(image.getWidth(), image.getHeight(),
            image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth()));
        return new com.google.zxing.qrcode.QRCodeReader().decode(
            new BinaryBitmap(new HybridBinarizer(source))).getText();
    }
    LocalDate today() { return LocalDate.now(ZoneId.of("Europe/Paris")); }

    @Test void registrationUsesBcryptAndDatabaseCreatedAtAndUniqueEmail() {
        RegisterRequest request = new RegisterRequest(); request.setFirstName("Alice"); request.setLastName("Fictive");
        request.setEmail("it-" + UUID.randomUUID() + "@example.invalid");
        String password = UUID.randomUUID().toString(); request.setPassword(password);
        User saved = accounts.register(request);
        User loaded = users.findById(saved.getIdUser()).orElseThrow();
        assertNotNull(loaded.getCreatedAt()); assertEquals("ACTIVE", loaded.getStatus());
        assertNotEquals(password, loaded.getPassword());
        assertNotNull(authentication.login(request.getEmail(), password));
        assertNull(authentication.login(request.getEmail(), UUID.randomUUID().toString()));
        assertThrows(IllegalArgumentException.class, () -> accounts.register(request));
        JdbcTemplate jdbc = new JdbcTemplate(datasource);
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
            "INSERT INTO users(first_name,last_name,email,password,status,id_role) VALUES ('Test','Fictif',?,'fixture','ACTIVE',1)",
            loaded.getEmail()));
    }
    @Test void cancellationReusesRegistrationButRevokesOldQr() throws Exception {
        User student = student(); Event event = event(student, today().plusDays(1), 2);
        int id = register(student,event); String old = token(student,id);
        enrollment.cancel(student.getEmail(), id);
        assertThrows(ResponseStatusException.class, () -> qr.png(student.getEmail(),id));
        assertEquals(id, register(student,event));
        assertNotEquals(old, token(student,id));
        assertEquals(1, registrations.countByEvent_IdEventAndStatus(event.getIdEvent(), "REGISTERED"));
    }
    @Test void tomorrowQrCanBeDisplayedButNotUsedToday() throws Exception {
        User student = student(); Event event = event(student,today().plusDays(1),2);
        int id = register(student,event); String token = token(student,id);
        var failure = assertThrows(ResponseStatusException.class,
            () -> scanner.scan("moderator@example.invalid",event.getIdEvent(),token));
        assertEquals(409,failure.getStatusCode().value());
        assertTrue(attendances.findByRegistration_IdRegistration(id).isEmpty());
    }
    @Test void concurrentStudentsCannotExceedLastPlace() throws Exception {
        User first=student(), second=student(); Event event=event(first,today().plusDays(1),1);
        ExecutorService executor=Executors.newFixedThreadPool(2);
        CountDownLatch start=new CountDownLatch(1);
        try {
            List<Future<Boolean>> results=new ArrayList<>();
            for(User user:List.of(first,second)) results.add(executor.submit(() -> {
                start.await(); try { enrollment.register(user.getEmail(),event.getIdEvent()); return true; }
                catch(ResponseStatusException rejected) { assertEquals(409,rejected.getStatusCode().value()); return false; }
            }));
            start.countDown(); int successes=0;
            for(var result:results) if(result.get(30,TimeUnit.SECONDS)) successes++;
            assertEquals(1,successes);
            assertEquals(1,registrations.countByEvent_IdEventAndStatus(event.getIdEvent(),"REGISTERED"));
        } finally {executor.shutdownNow();}
    }
    @Test void concurrentScansRecordOnlyOnePresenceAndEnforceSqlUniqueness() throws Exception {
        User student=student(); Event event=event(student,today(),2);
        int id=register(student,event); String token=token(student,id);
        ExecutorService executor=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1);
        try {
            Callable<String> call=() -> {start.await();return scanner.scan("moderator@example.invalid",event.getIdEvent(),token).code();};
            var first=executor.submit(call);var second=executor.submit(call);start.countDown();
            var codes=List.of(first.get(30,TimeUnit.SECONDS),second.get(30,TimeUnit.SECONDS));
            assertEquals(1,Collections.frequency(codes,"VALID"));assertEquals(1,Collections.frequency(codes,"ALREADY_USED"));
            JdbcTemplate jdbc=new JdbcTemplate(datasource);
            assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM attendances WHERE id_registration=?",Integer.class,id));
            assertThrows(DataIntegrityViolationException.class,()->jdbc.update(
                "INSERT INTO attendances(present,id_registration) VALUES (TRUE,?)",id));
            assertThrows(DataIntegrityViolationException.class,()->jdbc.update(
                "INSERT INTO registrations(status,id_user,id_event) VALUES ('REGISTERED',?,?)",student.getIdUser(),event.getIdEvent()));
            assertThrows(DataIntegrityViolationException.class,()->jdbc.update(
                "INSERT INTO qr_codes(code,id_registration) VALUES (?,?)","fictitious-"+UUID.randomUUID(),id));
        } finally {executor.shutdownNow();}
    }
}