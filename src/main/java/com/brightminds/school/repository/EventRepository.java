package com.brightminds.school.repository;

import com.brightminds.school.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {
    @Query("SELECT e FROM Event e WHERE e.startsAt >= :from AND e.startsAt <= :to ORDER BY e.startsAt ASC")
    List<Event> findByRange(Instant from, Instant to);
    List<Event> findAllByOrderByStartsAtDesc();
}
