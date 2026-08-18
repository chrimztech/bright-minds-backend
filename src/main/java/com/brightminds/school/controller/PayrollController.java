package com.brightminds.school.controller;

import com.brightminds.school.entity.PayrollPeriod;
import com.brightminds.school.entity.Payslip;
import com.brightminds.school.entity.enums.PayrollStatus;
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
@PreAuthorize("@perm.has('payroll:manage')")
public class PayrollController {

    private final PayrollPeriodRepository periodRepo;
    private final PayslipRepository payslipRepo;
    private final StaffRepository staffRepo;
    private final AuditService audit;

    @GetMapping("/periods")
    public List<PayrollPeriod> listPeriods() { return periodRepo.findAllByOrderByPeriodStartDesc(); }

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

    @PatchMapping("/periods/{id}/approve")
    public PayrollPeriod approve(@PathVariable UUID id) {
        PayrollPeriod p = periodRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Period not found"));
        p.setStatus(PayrollStatus.APPROVED);
        return periodRepo.save(p);
    }

    @PatchMapping("/periods/{id}/mark-paid")
    public PayrollPeriod markPaid(@PathVariable UUID id) {
        PayrollPeriod p = periodRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Period not found"));
        p.setStatus(PayrollStatus.PAID);
        return periodRepo.save(p);
    }

    @GetMapping("/payslips")
    public List<Payslip> listPayslips(
            @RequestParam(required = false) UUID periodId,
            @RequestParam(required = false) UUID staffId) {
        if (periodId != null) return payslipRepo.findByPeriodId(periodId);
        if (staffId != null) return payslipRepo.findByStaffId(staffId);
        return payslipRepo.findAll();
    }

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
