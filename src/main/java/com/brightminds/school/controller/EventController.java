package com.brightminds.school.controller;

import com.brightminds.school.entity.Event;
import com.brightminds.school.entity.enums.EventAudience;
import com.brightminds.school.repository.EventRepository;
import com.brightminds.school.repository.SchoolClassRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/events") @RequiredArgsConstructor @Tag(name = "Events / Calendar")
public class EventController {
    private final EventRepository repo;
    private final SchoolClassRepository classRepo;

    @GetMapping public List<Event> list() { return repo.findAllByOrderByStartsAtDesc(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Event create(@RequestBody EventRequest req) {
        var ev = Event.builder().title(req.getTitle()).description(req.getDescription())
                .startsAt(req.getStartsAt()).endsAt(req.getEndsAt()).location(req.getLocation())
                .audience(req.getAudience() != null ? req.getAudience() : EventAudience.ALL).build();
        if (req.getClassId() != null) classRepo.findById(req.getClassId()).ifPresent(ev::setSchoolClass);
        return repo.save(ev);
    }
    @PutMapping("/{id}") public Event update(@PathVariable UUID id, @RequestBody EventRequest req) {
        var ev = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Event not found"));
        ev.setTitle(req.getTitle()); ev.setDescription(req.getDescription());
        ev.setStartsAt(req.getStartsAt()); ev.setEndsAt(req.getEndsAt());
        ev.setLocation(req.getLocation());
        if (req.getAudience() != null) ev.setAudience(req.getAudience());
        return repo.save(ev);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) { repo.deleteById(id); }

    @Data public static class EventRequest {
        private String title; private String description; private Instant startsAt; private Instant endsAt;
        private String location; private EventAudience audience; private UUID classId;
    }
}
