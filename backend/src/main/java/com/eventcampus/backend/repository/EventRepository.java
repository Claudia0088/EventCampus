package com.eventcampus.backend.repository;

import com.eventcampus.backend.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Sort;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Integer> {
    long countByCategory_IdCategory(Integer idCategory);
    @EntityGraph(attributePaths = {"category", "creator"})
    List<Event> findAllBy(Sort sort);

    interface StatsCounts {
        long getPending();
        long getPublished();
        long getRejected();
        long getTotal();
    }

    @org.springframework.data.jpa.repository.Query(value = """
            SELECT COUNT(CASE WHEN BINARY status = 'EN_ATTENTE' THEN 1 END) AS pending,
                   COUNT(CASE WHEN BINARY status = 'PUBLIE' THEN 1 END) AS published,
                   COUNT(CASE WHEN BINARY status = 'REFUSE' THEN 1 END) AS rejected,
                   COUNT(*) AS total
            FROM events
            """, nativeQuery = true)
    StatsCounts getEventStats();

    @EntityGraph(attributePaths = {"category", "creator"})
    List<Event> findAllByStatus(String status, Sort sort);

    // One conditional statement also works with the existing MyISAM tables.
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @org.springframework.data.jpa.repository.Query(value = """
            UPDATE events SET status = :status, updated_at = CURRENT_TIMESTAMP
            WHERE id_event = :id AND BINARY status = 'EN_ATTENTE'
            """, nativeQuery = true)
    int decidePending(@org.springframework.data.repository.query.Param("id") Integer id,
            @org.springframework.data.repository.query.Param("status") String status);

    @EntityGraph(attributePaths = "category")
    List<Event> findByStatus(String status, Sort sort);

    @EntityGraph(attributePaths = "category")
    List<Event> findByCreator_IdUser(Integer idUser, Sort sort);
}
