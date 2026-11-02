package com.eventcampus.backend.repository;
import com.eventcampus.backend.entity.Registration;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface RegistrationRepository extends JpaRepository<Registration, Integer> {
    long countByEvent_IdEventAndStatus(Integer idEvent, String status);
    Optional<Registration> findByUser_IdUserAndEvent_IdEvent(Integer idUser, Integer idEvent);
    @EntityGraph(attributePaths = {"event", "event.category", "event.creator"})
    List<Registration> findByUser_IdUserOrderByRegisteredAtDesc(Integer idUser);
}
