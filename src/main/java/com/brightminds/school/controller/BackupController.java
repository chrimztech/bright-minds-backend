package com.brightminds.school.controller;

import com.brightminds.school.repository.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

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
