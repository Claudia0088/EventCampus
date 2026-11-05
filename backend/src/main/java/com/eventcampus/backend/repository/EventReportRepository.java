package com.eventcampus.backend.repository;

import com.eventcampus.backend.entity.EventReport;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface EventReportRepository extends JpaRepository<EventReport, Integer> {
    boolean existsByReporter_IdUserAndEvent_IdEvent(Integer reporter, Integer event);
    long countByReporter_IdUserAndCreatedAtGreaterThanEqual(Integer reporter, LocalDateTime since);
    @Override @EntityGraph(attributePaths = {"event"})
    Page<EventReport> findAll(Pageable pageable);
    @EntityGraph(attributePaths = {"event"})
    Page<EventReport> findByStatus(EventReport.Status status, Pageable pageable);
    @Override @EntityGraph(attributePaths = {"event", "reporter", "moderator"})
    Optional<EventReport> findById(Integer id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE event_reports SET status=:status, id_moderator=:moderator, processed_at=:processed
            WHERE id_report=:id AND BINARY status='PENDING'
            """, nativeQuery = true)
    int decidePending(@Param("id") Integer id, @Param("status") String status,
            @Param("moderator") Integer moderator, @Param("processed") LocalDateTime processed);
}
