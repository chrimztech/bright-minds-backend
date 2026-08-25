package com.brightminds.school.controller;

import com.brightminds.school.entity.Inquiry;
import com.brightminds.school.repository.InquiryRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

// Staff-facing management of inquiries submitted through the public landing page. The
// anonymous submission endpoint itself lives on PublicController (POST /public/inquiries,
// permitAll) — kept separate from this authenticated /inquiries path so the two very
// different trust levels never share a single @PreAuthorize-guarded method.
@RestController
@RequestMapping("/inquiries")
@RequiredArgsConstructor
@Tag(name = "Inquiries")
public class InquiryController {
    private final InquiryRepository repo;

    @PreAuthorize("@perm.has('inquiries:view') or @perm.has('inquiries:manage')")
    @GetMapping
    public List<Inquiry> list() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @PreAuthorize("@perm.has('inquiries:manage')")
    @PatchMapping("/{id}/read")
    public Inquiry markRead(@PathVariable UUID id) {
        Inquiry i = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Inquiry not found"));
        i.setRead(true);
        return repo.save(i);
    }

    @PreAuthorize("@perm.has('inquiries:manage')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        repo.deleteById(id);
    }
}
