package com.brightminds.school.controller;

import com.brightminds.school.entity.Document;
import com.brightminds.school.repository.DocumentRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Tag(name = "Documents")
public class DocumentController {

    private final DocumentRepository repo;

    @GetMapping
    public List<Document> list() {
        return repo.findAll();
    }

    @PreAuthorize("@perm.has('documents:manage')")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Document create(@RequestBody DocReq req) {
        return repo.save(Document.builder()
                .title(req.getTitle())
                .url(req.getUrl())
                .category(req.getCategory())
                .description(req.getDescription())
                .build());
    }

    @PreAuthorize("@perm.has('documents:manage')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        if (!repo.existsById(id)) throw new EntityNotFoundException("Document not found");
        repo.deleteById(id);
    }

    @Data
    public static class DocReq {
        private String title, url, category, description;
    }
}
