package com.eventcampus.backend;
import com.eventcampus.backend.entity.*;
import com.eventcampus.backend.repository.*;
import com.eventcampus.backend.service.RegistrationService;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RegistrationServiceTests {
    RegistrationRepository registrations = mock(RegistrationRepository.class);
    EventRepository events = mock(EventRepository.class);
    UserRepository users = mock(UserRepository.class);
    RegistrationWriteLock lock = mock(RegistrationWriteLock.class);
    QrCodeRepository codes = mock(QrCodeRepository.class);
    RegistrationService service = new RegistrationService(registrations, events, users, lock, codes);
    Map<Integer,Registration> rows = new HashMap<>();
    Event event; User first, second;
    @BeforeEach void setup() {
        var role = new Role(); role.setName("USER");
        first = new User(); first.setIdUser(1); first.setRole(role); first.setStatus("ACTIVE");
        second = new User(); second.setIdUser(2); second.setRole(role); second.setStatus("ACTIVE");
        when(users.findByEmail("first@test")).thenReturn(Optional.of(first)); when(users.findByEmail("second@test")).thenReturn(Optional.of(second));
        var category = new Category(); category.setIdCategory(4); category.setName("Formation");
        event = new Event(); event.setIdEvent(10); event.setTitle("Event"); event.setCreator(first); event.setCategory(category);
        event.setStatus("PUBLIE"); event.setCapacity(1); event.setEventDate(LocalDate.now().plusDays(5)); event.setEventTime(LocalTime.NOON);
        when(events.findById(10)).thenReturn(Optional.of(event));
        when(registrations.countByEvent_IdEventAndStatus(10,"REGISTERED")).thenAnswer(call -> rows.values().stream().filter(r -> "REGISTERED".equals(r.getStatus())).count());
        when(registrations.findByUser_IdUserAndEvent_IdEvent(anyInt(),eq(10))).thenAnswer(call -> Optional.ofNullable(rows.get(call.getArgument(0))));
        when(registrations.findById(anyInt())).thenAnswer(call -> rows.values().stream().filter(r -> r.getIdRegistration().equals(call.getArgument(0))).findFirst());
        when(registrations.findByUser_IdUserOrderByRegisteredAtDesc(anyInt())).thenAnswer(call -> rows.values().stream().filter(r -> r.getUser().getIdUser().equals(call.getArgument(0))).toList());
        when(registrations.saveAndFlush(any())).thenAnswer(call -> { Registration r = call.getArgument(0); r.setIdRegistration(r.getUser().getIdUser()); rows.put(r.getUser().getIdUser(),r); return r; });
        Object monitor = new Object();
        when(lock.execute(eq(10),any())).thenAnswer(call -> { synchronized(monitor) { return ((Supplier<?>)call.getArgument(1)).get(); } });
    }
    @Test void registrationAndCancellationReleasePlaceAndPreserveRow() {
        service.register("first@test",10); assertEquals("REGISTERED",rows.get(1).getStatus());
        assertEquals(1,service.mine("first@test").size()); assertTrue(service.mine("second@test").isEmpty());
        assertThrows(ResponseStatusException.class,()->service.register("first@test",10));
        assertThrows(ResponseStatusException.class,()->service.register("second@test",10));
        service.cancel("first@test",1); assertEquals("CANCELLED",rows.get(1).getStatus());
        var order = inOrder(codes, registrations);
        order.verify(codes).revoke(1);
        order.verify(registrations).saveAndFlush(any());
        service.register("second@test",10); assertEquals(2,rows.size()); verify(registrations,never()).delete(any());
    }
    @Test void reusesCancelledRow() {
        service.register("first@test",10); service.cancel("first@test",1); service.register("first@test",10);
        assertEquals(1,rows.size()); assertEquals(1,rows.get(1).getIdRegistration()); assertEquals("REGISTERED",rows.get(1).getStatus());
    }
    @Test void rejectsPrivateCancellationAndRepeatedCancellation() {
        service.register("first@test",10);
        assertThrows(ResponseStatusException.class,()->service.cancel("second@test",1)); assertEquals("REGISTERED",rows.get(1).getStatus());
        service.cancel("first@test",1); assertThrows(ResponseStatusException.class,()->service.cancel("first@test",1));
    }
    @Test void rejectsDisabledMissingUnpublishedAndPastEvents() {
        first.setStatus("INACTIVE"); assertThrows(ResponseStatusException.class,()->service.register("first@test",10)); first.setStatus("ACTIVE");
        when(lock.execute(eq(999),any())).thenAnswer(c -> ((Supplier<?>)c.getArgument(1)).get());
        assertThrows(ResponseStatusException.class,()->service.register("first@test",999));
        for(var status:List.of("EN_ATTENTE","REFUSE")) { event.setStatus(status); assertThrows(ResponseStatusException.class,()->service.register("first@test",10)); }
        event.setStatus("PUBLIE"); event.setEventDate(LocalDate.now().minusDays(1)); assertThrows(ResponseStatusException.class,()->service.register("first@test",10));
        verify(registrations,never()).saveAndFlush(any());
    }
    @Test void twoStudentsCompeteForLastPlaceOnlyOneSucceeds() throws Exception {
        var start=new CountDownLatch(1); var pool=Executors.newFixedThreadPool(2);
        try {
            var results=new ArrayList<Future<Boolean>>();
            for(var email:List.of("first@test","second@test")) results.add(pool.submit(()->{start.await();try{service.register(email,10);return true;}catch(ResponseStatusException error){return false;}}));
            start.countDown(); int succeeded=0; for(var result:results) if(result.get(5,TimeUnit.SECONDS))succeeded++;
            assertEquals(1,succeeded); assertEquals(1,rows.size());
        } finally { pool.shutdownNow(); }
    }
}
