package com.brightminds.school.controller;

import com.brightminds.school.repository.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/admin/backup")
@RequiredArgsConstructor
@Tag(name = "Backup")
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
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

    @Value("${spring.datasource.url}")
    private String datasourceUrl;
    @Value("${spring.datasource.username}")
    private String datasourceUsername;
    @Value("${spring.datasource.password}")
    private String datasourcePassword;
    @Value("${app.backup.pg-dump-path}")
    private String pgDumpPath;

    private static final Pattern JDBC_URL = Pattern.compile("jdbc:postgresql://([^:/]+)(?::(\\d+))?/([^?]+)");

    @GetMapping("/sql")
    public ResponseEntity<ByteArrayResource> downloadSql() {
        Matcher m = JDBC_URL.matcher(datasourceUrl);
        if (!m.matches()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not parse datasource URL for backup: " + datasourceUrl);
        }
        String host = m.group(1);
        String port = m.group(2) != null ? m.group(2) : "5432";
        String dbName = m.group(3);

        Path tmp;
        try {
            tmp = Files.createTempFile("school-backup-", ".sql");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not create temp file for backup", e);
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    pgDumpPath, "-h", host, "-p", port, "-U", datasourceUsername,
                    "--no-owner", "--no-privileges", "-F", "p", "-f", tmp.toString(), dbName);
            pb.environment().put("PGPASSWORD", datasourcePassword);
            pb.redirectErrorStream(true);
            Process proc = pb.start();
            String log = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = proc.waitFor(120, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "pg_dump timed out after 120s");
            }
            if (proc.exitValue() != 0) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "pg_dump failed (exit " + proc.exitValue() + "): " + log);
            }
            byte[] data = Files.readAllBytes(tmp);
            String filename = "school-full-backup-" + DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HHmmss")
                    .format(java.time.LocalDateTime.now()) + ".sql";
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("application/sql"))
                    .body(new ByteArrayResource(data));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not run pg_dump (is it installed and on PATH? override with PG_DUMP_PATH): " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Backup interrupted", e);
        } finally {
            try { Files.deleteIfExists(tmp); } catch (IOException ignored) { }
        }
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
