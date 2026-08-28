package com.brightminds.school.controller;

import com.brightminds.school.repository.*;
import com.brightminds.school.service.AuditService;
import com.brightminds.school.service.BackupService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/backup")
@RequiredArgsConstructor
@Tag(name = "Backup")
@PreAuthorize("@perm.has('backup:create')")
public class BackupController {

    private final PupilRepository pupils;
    private final StaffRepository staff;
    private final AttendanceRepository attendance;
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final MarkRepository marks;
    private final ExamRepository exams;
    private final AnnouncementRepository announcements;
    private final GuardianRepository guardians;
    private final SchoolClassRepository classes;
    private final LibraryBookRepository libraryBooks;
    private final LibraryLoanRepository libraryLoans;
    private final VehicleRepository vehicles;
    private final ExpenseRepository expenses;
    private final PayslipRepository payslips;
    private final DisciplineRecordRepository discipline;
    private final HealthRecordRepository health;
    private final DocumentRepository documents;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final AuditLogRepository auditLogs;
    private final AuditService audit;
    private final BackupService backupService;

    @GetMapping("/sql")
    public ResponseEntity<ByteArrayResource> downloadSql() {
        byte[] data = backupService.runPgDump(backupService.parseDbTarget());
        String filename = "school-database-" + DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss")
                .format(java.time.LocalDateTime.now()) + ".sql";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/sql"))
                .body(new ByteArrayResource(data));
    }

    // The one backup meant for disaster recovery: the full database (schema + every row) AND
    // every uploaded file (staff photos/signatures, documents, hero images, etc.) bundled into
    // a single archive — restoring only the database would leave every image and document
    // reference in the app pointing at files that no longer exist.
    @GetMapping("/full")
    public ResponseEntity<ByteArrayResource> downloadFull() {
        byte[] zip = backupService.buildFullBackupZip();
        String filename = "school-full-backup-" + DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss")
                .format(java.time.LocalDateTime.now()) + ".zip";
        audit.log("DOWNLOAD_BACKUP", "System", null, "Full backup (database + uploaded files)");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(new ByteArrayResource(zip));
    }

    // Every automatic snapshot the system has taken on its own — the nightly scheduled backup,
    // and the safety-net snapshot restore() below always takes of the *current* state right
    // before overwriting it. Surfacing these means a bad restore is itself always recoverable,
    // and nobody has to remember to click "Download full backup" for the nightly one to exist.
    @GetMapping("/scheduled")
    public List<BackupService.SnapshotInfo> listScheduled() {
        return backupService.listSnapshots();
    }

    @GetMapping("/scheduled/{filename}")
    public ResponseEntity<ByteArrayResource> downloadScheduled(@PathVariable String filename) {
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }
        Path path = backupService.resolveSnapshot(filename);
        byte[] data;
        try {
            data = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not read backup file", e);
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(new ByteArrayResource(data));
    }

    // Restoring is the one genuinely destructive action in this whole controller — it drops
    // and recreates the live schema and overwrites uploaded files — so it's deliberately kept
    // on its own stricter permission, separate from the read-only "can make a backup"
    // permission everyone with backup:create has.
    @PreAuthorize("@perm.has('backup:restore')")
    @PostMapping("/restore")
    public Map<String, Object> restore(@RequestParam("file") MultipartFile file) {
        // Whatever is about to be overwritten gets saved first — if the uploaded archive turns
        // out to be the wrong one, or restoring it goes badly, this snapshot is what undoes it.
        Path safetyNet = backupService.saveSnapshot("pre-restore");
        BackupService.RestoreResult result;
        try {
            result = backupService.restoreFromZip(file.getInputStream());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Could not read the uploaded file: " + e.getMessage(), e);
        }
        audit.log("RESTORE_BACKUP", "System", null,
                result.filesRestored() + " file(s) restored; safety snapshot saved as " + safetyNet.getFileName());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("filesRestored", result.filesRestored());
        response.put("restoredAt", java.time.Instant.now().toString());
        response.put("safetyBackup", safetyNet.getFileName().toString());
        response.put("note", "Restart the backend now so every service picks up the restored data cleanly.");
        return response;
    }

    @GetMapping
    public Map<String, Object> backup() {
        Map<String, Object> tables = new LinkedHashMap<>();
        tables.put("pupils", pupils.findAll());
        tables.put("staff", staff.findAll());
        tables.put("classes", classes.findAll());
        tables.put("attendance", attendance.findAll());
        tables.put("invoices", invoices.findAll());
        tables.put("payments", payments.findAll());
        tables.put("exams", exams.findAll());
        tables.put("marks", marks.findAll());
        tables.put("announcements", announcements.findAll());
        tables.put("guardians", guardians.findAll());
        tables.put("library_books", libraryBooks.findAll());
        tables.put("library_loans", libraryLoans.findAll());
        tables.put("vehicles", vehicles.findAll());
        tables.put("expenses", expenses.findAll());
        tables.put("payslips", payslips.findAll());
        tables.put("discipline", discipline.findAll());
        tables.put("health", health.findAll());
        tables.put("documents", documents.findAll());
        tables.put("roles", roles.findAll());
        tables.put("permissions", permissions.findAll());
        tables.put("audit_logs", auditLogs.findAll());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tables", tables);
        result.put("exportedAt", java.time.Instant.now().toString());
        return result;
    }
}
