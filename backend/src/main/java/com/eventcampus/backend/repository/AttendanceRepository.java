package com.eventcampus.backend.repository;

import com.eventcampus.backend.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {
    Optional<Attendance> findByRegistration_IdRegistration(Integer id);
}
