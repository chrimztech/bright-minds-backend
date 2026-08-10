package com.brightminds.school.controller;

import com.brightminds.school.entity.Announcement;
import com.brightminds.school.repository.AnnouncementRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
@Tag(name = "Announcements")
public class AnnouncementController {

    private final AnnouncementRepository repo;

    @GetMapping
    public List<Announcement> list() { return repo.findAllByOrderByCreatedAtDesc(); }

    @GetMapping("/{id}")
    public Announcement getById(@PathVariable UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Announcement not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Announcement create(@RequestBody AnnouncementRequest req) {
        return repo.save(Announcement.builder()
                .title(req.getTitle())
                .body(req.getBody())
                .audience(req.getAudience() != null ? req.getAudience() : "all")
                .build());
    }

    @PutMapping("/{id}")
    public Announcement update(@PathVariable UUID id, @RequestBody AnnouncementRequest req) {
        Announcement a = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Not found"));
        a.setTitle(req.getTitle());
        a.setBody(req.getBody());
        if (req.getAudience() != null) a.setAudience(req.getAudience());
        return repo.save(a);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { repo.deleteById(id); }

    @Data
    public static class AnnouncementRequest {
        @NotBlank private String title;
        @NotBlank private String body;
        private String audience;
    }
}
