package com.brightminds.school.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

// Local-disk file storage used across the app (staff photos/signatures, homework attachments,
// etc.) — there's no cloud storage account configured, so uploads land in a directory on this
// server and are served back from GET /uploads/{filename}, which is deliberately public
// (see SecurityConfig) since <img> tags — including inside printed documents — never send
// an Authorization header.
@RestController
@RequestMapping("/uploads")
@Tag(name = "File Uploads")
public class FileUploadController {

    // Images (staff/pupil photos, signatures) plus common homework-attachment document types —
    // this endpoint was image-only, which silently rejected every PDF/Word worksheet a teacher
    // tried to attach to homework.
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp", "image/svg+xml",
            "application/pdf", "text/plain",
            "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    @PreAuthorize("isAuthenticated()")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UploadResponse upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("File is empty");
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only images, PDFs, Word/Excel/PowerPoint documents or plain text files are allowed");
        }
        String ext = switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/svg+xml" -> ".svg";
            case "application/pdf" -> ".pdf";
            case "text/plain" -> ".txt";
            case "application/msword" -> ".doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> ".docx";
            case "application/vnd.ms-powerpoint" -> ".ppt";
            case "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> ".pptx";
            case "application/vnd.ms-excel" -> ".xls";
            case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> ".xlsx";
            default -> "";
        };
        String filename = UUID.randomUUID() + ext;

        Path dir = Paths.get(uploadsDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        Path target = dir.resolve(filename).normalize();
        file.transferTo(target);

        return new UploadResponse("/api/uploads/" + filename);
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Resource> serve(@PathVariable String filename) throws MalformedURLException {
        Path dir = Paths.get(uploadsDir).toAbsolutePath().normalize();
        Path file = dir.resolve(filename).normalize();
        // Reject any path that escapes the uploads directory (e.g. "../../etc/passwd").
        if (!file.startsWith(dir) || !Files.exists(file)) {
            throw new EntityNotFoundException("File not found: " + filename);
        }
        Resource resource = new UrlResource(file.toUri());
        String contentType;
        try {
            contentType = Files.probeContentType(file);
        } catch (IOException e) {
            contentType = null;
        }
        return ResponseEntity.ok()
                .contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                .header("Cache-Control", "public, max-age=31536000, immutable")
                .body(resource);
    }

    @Data
    public static class UploadResponse {
        private final String url;
    }
}
