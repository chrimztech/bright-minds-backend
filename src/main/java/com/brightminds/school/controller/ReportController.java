package com.brightminds.school.controller;

import com.brightminds.school.entity.enums.LoanStatus;
import com.brightminds.school.entity.enums.PaymentStatus;
import com.brightminds.school.repository.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Reports")
@PreAuthorize("@perm.has('reports:view')")
public class ReportController {

    private final PupilRepository pupils;
    private final StaffRepository staff;
    private final SchoolClassRepository classes;
    private final InvoiceRepository invoices;
    private final PaymentRepository payments;
    private final ExpenseRepository expenses;
    private final LibraryBookRepository libraryBooks;
    private final LibraryLoanRepository libraryLoans;
    private final AttendanceRepository attendance;
    private final AdmissionRepository admissions;
    private final VehicleRepository vehicles;
    private final HealthRecordRepository healthRecords;

    @GetMapping("/summary")
    public ReportSummary summary() {
        ReportSummary r = new ReportSummary();
        r.setTotalPupils(pupils.count());
        r.setTotalStaff(staff.count());
        r.setTotalClasses(classes.count());
        r.setFeesCollected(payments.findAll().stream()
                .filter(p -> p.getStatus() == PaymentStatus.CONFIRMED)
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue());
        r.setFeesBilled(invoices.findAll().stream()
                .map(i -> i.getTotal() != null ? i.getTotal() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue());
        r.setTotalExpenses(expenses.findAll().stream()
                .map(e -> e.getAmount() != null ? e.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add).doubleValue());
        r.setLibraryBooks(libraryBooks.count());
        r.setActiveLoans(libraryLoans.findByStatus(LoanStatus.BORROWED).size());
        r.setAttendancePresent(attendance.count());
        r.setTotalAdmissions(admissions.count());
        r.setTotalVehicles(vehicles.count());
        r.setClinicVisits(healthRecords.count());
        return r;
    }

    @Data
    public static class ReportSummary {
        private long totalPupils, totalStaff, totalClasses, libraryBooks, activeLoans,
                attendancePresent, totalAdmissions, totalVehicles, clinicVisits;
        private double feesCollected, feesBilled, totalExpenses;
    }
}
