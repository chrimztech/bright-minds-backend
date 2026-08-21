package com.brightminds.school.controller;

import com.brightminds.school.entity.Expense;
import com.brightminds.school.entity.PayrollPeriod;
import com.brightminds.school.entity.Payslip;
import com.brightminds.school.entity.enums.PayrollStatus;
import com.brightminds.school.repository.ExpenseRepository;
import com.brightminds.school.repository.PayrollPeriodRepository;
import com.brightminds.school.repository.PayslipRepository;
import com.brightminds.school.repository.StaffRepository;
import com.brightminds.school.service.AuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payroll")
@RequiredArgsConstructor
@Tag(name = "Payroll")
public class PayrollController {

    private final PayrollPeriodRepository periodRepo;
    private final PayslipRepository payslipRepo;
    private final StaffRepository staffRepo;
    private final ExpenseRepository expenseRepo;
    private final AuditService audit;

    // payroll:view alone (without :manage) previously granted no access at all — every
    // endpoint including these GETs was gated behind :manage, so a role deliberately given
    // view-only rights via Roles & Permissions was silently blocked from viewing anything.
    @PreAuthorize("@perm.has('payroll:view') or @perm.has('payroll:manage')")
    @GetMapping("/periods")
    public List<PayrollPeriod> listPeriods() { return periodRepo.findAllByOrderByPeriodStartDesc(); }

    @PreAuthorize("@perm.has('payroll:manage')")
    @PostMapping("/periods")
    @ResponseStatus(HttpStatus.CREATED)
    public PayrollPeriod createPeriod(@RequestBody PeriodRequest req) {
        return periodRepo.save(PayrollPeriod.builder()
                .periodLabel(req.getPeriodLabel())
                .periodStart(req.getPeriodStart())
                .periodEnd(req.getPeriodEnd())
                .notes(req.getNotes())
                .build());
    }

    @PreAuthorize("@perm.has('payroll:manage')")
    @PatchMapping("/periods/{id}/approve")
    public PayrollPeriod approve(@PathVariable UUID id) {
        PayrollPeriod p = periodRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Period not found"));
        p.setStatus(PayrollStatus.APPROVED);
        return periodRepo.save(p);
    }

    // Records a "Salaries" Expense for this period's total net pay — previously marking a
    // period paid had no connection to Accounts at all, so the school's largest recurring
    // expense category never actually showed up in the books unless someone separately
    // re-entered it by hand.
    @PreAuthorize("@perm.has('payroll:manage')")
    @PatchMapping("/periods/{id}/mark-paid")
    public PayrollPeriod markPaid(@PathVariable UUID id) {
        PayrollPeriod p = periodRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Period not found"));
        if (p.getStatus() == PayrollStatus.PAID) throw new IllegalArgumentException("This period has already been marked paid");

        List<Payslip> slips = payslipRepo.findByPeriodId(id);
        BigDecimal total = slips.stream().map(Payslip::getNetPay).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() > 0) {
            expenseRepo.save(Expense.builder()
                    .category("Salaries")
                    .amount(total)
                    .description("Payroll — " + p.getPeriodLabel() + " (" + slips.size() + " payslip(s))")
                    .paymentMethod("BANK")
                    .refNo(p.getPeriodLabel())
                    .spentOn(p.getPeriodEnd() != null ? p.getPeriodEnd() : LocalDate.now())
                    .build());
        }

        p.setStatus(PayrollStatus.PAID);
        PayrollPeriod saved = periodRepo.save(p);
        audit.log("MARK_PAYROLL_PAID", "PayrollPeriod", id.toString(),
                slips.size() + " payslip(s) totalling " + total + " recorded as a Salaries expense");
        return saved;
    }

    @PreAuthorize("@perm.has('payroll:view') or @perm.has('payroll:manage')")
    @GetMapping("/payslips")
    public List<Payslip> listPayslips(
            @RequestParam(required = false) UUID periodId,
            @RequestParam(required = false) UUID staffId) {
        if (periodId != null) return payslipRepo.findByPeriodId(periodId);
        if (staffId != null) return payslipRepo.findByStaffId(staffId);
        return payslipRepo.findAll();
    }

    @PreAuthorize("@perm.has('payroll:manage')")
    @PostMapping("/payslips")
    @ResponseStatus(HttpStatus.CREATED)
    public Payslip createPayslip(@RequestBody PayslipRequest req) {
        var staff = staffRepo.findById(req.getStaffId()).orElseThrow(() -> new EntityNotFoundException("Staff not found"));
        var period = periodRepo.findById(req.getPeriodId()).orElseThrow(() -> new EntityNotFoundException("Period not found"));
        BigDecimal net = req.getBasic().add(req.getAllowances()).subtract(req.getDeductions()).subtract(req.getTax());
        return payslipRepo.save(Payslip.builder()
                .staff(staff).period(period)
                .basic(req.getBasic()).allowances(req.getAllowances())
                .deductions(req.getDeductions()).tax(req.getTax())
                .netPay(net).notes(req.getNotes())
                .build());
    }

    @PreAuthorize("@perm.has('payroll:reverse')")
    @PutMapping("/payslips/{id}")
    public Payslip updatePayslip(@PathVariable UUID id, @RequestBody PayslipRequest req) {
        Payslip p = payslipRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Payslip not found"));
        BigDecimal oldNet = p.getNetPay();
        if (req.getBasic() != null) p.setBasic(req.getBasic());
        if (req.getAllowances() != null) p.setAllowances(req.getAllowances());
        if (req.getDeductions() != null) p.setDeductions(req.getDeductions());
        if (req.getTax() != null) p.setTax(req.getTax());
        if (req.getNotes() != null) p.setNotes(req.getNotes());
        p.setNetPay(p.getBasic().add(p.getAllowances()).subtract(p.getDeductions()).subtract(p.getTax()));
        audit.log("CORRECT_PAYSLIP", "Payslip", id.toString(),
                "net pay " + oldNet + " -> " + p.getNetPay() + " (staff " + p.getStaff().getFullName() + ")");
        return payslipRepo.save(p);
    }

    @PreAuthorize("@perm.has('payroll:reverse')")
    @DeleteMapping("/payslips/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayslip(@PathVariable UUID id) {
        Payslip p = payslipRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Payslip not found"));
        audit.log("DELETE_PAYSLIP", "Payslip", id.toString(),
                "net pay " + p.getNetPay() + " (staff " + p.getStaff().getFullName() + ")");
        payslipRepo.deleteById(id);
    }

    @Data public static class PeriodRequest {
        private String periodLabel;
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private String notes;
    }

    @Data public static class PayslipRequest {
        private UUID staffId;
        private UUID periodId;
        private BigDecimal basic = BigDecimal.ZERO;
        private BigDecimal allowances = BigDecimal.ZERO;
        private BigDecimal deductions = BigDecimal.ZERO;
        private BigDecimal tax = BigDecimal.ZERO;
        private String notes;
    }
}
