package com.brightminds.school.controller;

import com.brightminds.school.entity.Announcement;
import com.brightminds.school.repository.AnnouncementRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
@Tag(name = "Announcements")
public class AnnouncementController {

    private static final Set<String> MANAGE_ROLES = Set.of(
            "ROLE_SUPER_ADMIN", "ROLE_ADMIN", "ROLE_HEAD_TEACHER", "ROLE_DEPUTY_HEAD");

    private final AnnouncementRepository repo;

    @GetMapping
    public List<Announcement> list(@AuthenticationPrincipal UserDetails principal) {
        List<Announcement> all = repo.findAllByOrderByCreatedAtDesc();
        if (principal == null) return all;
        Set<String> authorities = principal.getAuthorities().stream()
                .map(a -> a.getAuthority()).collect(Collectors.toSet());
        // Admin/management roles manage announcements, so they see every audience.
        if (authorities.stream().anyMatch(MANAGE_ROLES::contains)) return all;
        boolean isParentOnly = authorities.stream().allMatch(a -> a.equals("ROLE_PARENT"));
        String targetAudience = isParentOnly ? "parents" : "staff";
        return all.stream()
                .filter(a -> a.getAudience() == null
                        || a.getAudience().equalsIgnoreCase("all")
                        || a.getAudience().equalsIgnoreCase(targetAudience))
                .toList();
    }

    @GetMapping("/{id}")
    public Announcement getById(@PathVariable UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Announcement not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
    public Announcement create(@RequestBody AnnouncementRequest req) {
        return repo.save(Announcement.builder()
                .title(req.getTitle())
                .body(req.getBody())
                .audience(req.getAudience() != null ? req.getAudience() : "all")
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
    public Announcement update(@PathVariable UUID id, @RequestBody AnnouncementRequest req) {
        Announcement a = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Not found"));
        a.setTitle(req.getTitle());
        a.setBody(req.getBody());
        if (req.getAudience() != null) a.setAudience(req.getAudience());
        return repo.save(a);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
    public void delete(@PathVariable UUID id) { repo.deleteById(id); }

    @Data
    public static class AnnouncementRequest {
        @NotBlank private String title;
        @NotBlank private String body;
        private String audience;
    }
}
