package com.brightminds.school.controller;

import com.brightminds.school.entity.*;
import com.brightminds.school.entity.enums.InvoiceStatus;
import com.brightminds.school.entity.enums.PaymentMethod;
import com.brightminds.school.entity.enums.PaymentStatus;
import com.brightminds.school.repository.*;
import com.brightminds.school.service.AuditService;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/fees")
@RequiredArgsConstructor
@Tag(name = "Fees & Payments")
@PreAuthorize("@perm.has('fees:view')")
public class FeeController {

    private final FeeItemRepository feeItemRepo;
    private final InvoiceRepository invoiceRepo;
    private final PaymentRepository paymentRepo;
    private final PupilRepository pupilRepo;
    private final TermRepository termRepo;
    private final SchoolClassRepository classRepo;
    private final AuditService audit;

    // ─── Fee Items ────────────────────────────────────────────────────────────

    @GetMapping("/items")
    public List<FeeItem> listItems() { return feeItemRepo.findAll(); }

    @PreAuthorize("@perm.has('fees:configure')")
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public FeeItem createItem(@RequestBody FeeItemRequest req) {
        FeeItem item = FeeItem.builder()
                .name(req.getName())
                .amount(req.getAmount() != null ? req.getAmount() : BigDecimal.ZERO)
                .isRecurring(req.isRecurring())
                .dueDate(req.getDueDate())
                .build();
        item.setCategory(req.getCategory() != null ? req.getCategory() : "SCHOOL_FEE");
        if (req.getClassId() != null) classRepo.findById(req.getClassId()).ifPresent(item::setSchoolClass);
        if (req.getTermId() != null) termRepo.findById(req.getTermId()).ifPresent(item::setTerm);
        return feeItemRepo.save(item);
    }

    @PreAuthorize("@perm.has('fees:configure')")
    @PutMapping("/items/{id}")
    public FeeItem updateItem(@PathVariable UUID id, @RequestBody FeeItemRequest req) {
        FeeItem item = feeItemRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fee item not found"));
        if (req.getName() != null) item.setName(req.getName());
        if (req.getAmount() != null) item.setAmount(req.getAmount());
        if (req.getCategory() != null) item.setCategory(req.getCategory());
        if (req.getDueDate() != null) item.setDueDate(req.getDueDate());
        return feeItemRepo.save(item);
    }

    @PreAuthorize("@perm.has('fees:configure')")
    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable UUID id) { feeItemRepo.deleteById(id); }

    // ─── Invoices ─────────────────────────────────────────────────────────────

    @GetMapping("/invoices")
    public List<Invoice> listInvoices(
            @RequestParam(required = false) UUID pupilId,
            @RequestParam(required = false) UUID termId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) String grade) {
        List<Invoice> base;
        if (pupilId != null) base = invoiceRepo.findByPupilIdOrderByCreatedAtDesc(pupilId);
        else if (termId != null) base = invoiceRepo.findByTerm_IdOrderByCreatedAtDesc(termId);
        else base = invoiceRepo.findAllByOrderByCreatedAtDesc();
        return filterByClassAndGrade(base, Invoice::getPupil, classId, grade);
    }

    @GetMapping("/invoices/{id}")
    public Invoice getInvoice(@PathVariable UUID id) {
        return invoiceRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
    }

    // No way existed to remove an invoice at all (e.g. one created by mistake with no
    // payments against it) — it would sit permanently. Only allowed once every payment
    // against it has been reversed/removed first, both to avoid a FK violation (Payment.invoice
    // is not cascade-deleted) and to keep the audit trail intact for any invoice real money
    // actually touched.
    @PreAuthorize("@perm.has('fees:reverse')")
    @DeleteMapping("/invoices/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInvoice(@PathVariable UUID id) {
        Invoice invoice = invoiceRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
        if (!paymentRepo.findByInvoiceIdOrderByPaidOnDesc(id).isEmpty()) {
            throw new IllegalArgumentException("Cannot delete an invoice with payments recorded against it — reverse the payment(s) first.");
        }
        audit.log("DELETE_INVOICE", "Invoice", id.toString(),
                "invoiceNo " + invoice.getInvoiceNo() + ", total " + invoice.getTotal());
        invoiceRepo.deleteById(id);
    }

    @PreAuthorize("@perm.has('fees:create')")
    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    public Invoice createInvoice(@RequestBody InvoiceRequest req) {
        return saveInvoice(req);
    }

    @PreAuthorize("@perm.has('fees:create')")
    @PostMapping("/invoices/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Invoice> bulkCreateInvoices(@RequestBody List<InvoiceRequest> reqs) {
        return reqs.stream().map(this::saveInvoice).toList();
    }

    // Runs daily at 01:00 — see also POST /fees/invoices/apply-late-fees for an on-demand trigger.
    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 1 * * *")
    public void scheduledLateFees() { applyLateFees(); }

    @PreAuthorize("@perm.has('fees:create')")
    @PostMapping("/invoices/apply-late-fees")
    public List<Invoice> applyLateFees() {
        List<Invoice> overdue = invoiceRepo.findByDueDateBeforeAndStatusInAndLateFeeAppliedFalse(
                LocalDate.now(), List.of(InvoiceStatus.UNPAID, InvoiceStatus.PARTIAL));
        List<Invoice> created = new java.util.ArrayList<>();
        // Tagged with a shared LATE_FEE-category fee item (not left null) so these invoices
        // show up as their own bucket in Accounts instead of silently falling into School Fees.
        FeeItem lateFeeItem = feeItemRepo.findByCategoryAndNameAndTermIsNull("LATE_FEE", "Administrative Late Fee")
                .orElseGet(() -> feeItemRepo.save(FeeItem.builder()
                        .name("Administrative Late Fee")
                        .category("LATE_FEE")
                        .amount(LATE_FEE_AMOUNT)
                        .isRecurring(false)
                        .build()));
        for (Invoice original : overdue) {
            Invoice lateFee = Invoice.builder()
                    .invoiceNo(nextInvoiceNo())
                    .pupil(original.getPupil())
                    .term(original.getTerm())
                    .feeItem(lateFeeItem)
                    .total(LATE_FEE_AMOUNT)
                    .description("Administrative late payment fee — overdue invoice " + original.getInvoiceNo())
                    .dueDate(LocalDate.now())
                    .build();
            created.add(invoiceRepo.save(lateFee));
            original.setLateFeeApplied(true);
            invoiceRepo.save(original);
        }
        return created;
    }

    private static final BigDecimal LATE_FEE_AMOUNT = BigDecimal.valueOf(35);

    private String nextInvoiceNo() {
        return "INV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    private Invoice saveInvoice(InvoiceRequest req) {
        Pupil pupil = pupilRepo.findById(req.getPupilId())
                .orElseThrow(() -> new EntityNotFoundException("Pupil not found: " + req.getPupilId()));
        String invNo = req.getInvoiceNo() != null && !req.getInvoiceNo().isBlank()
                ? req.getInvoiceNo()
                : nextInvoiceNo();
        Invoice inv = Invoice.builder()
                .invoiceNo(invNo)
                .pupil(pupil)
                .total(req.getTotal() != null ? req.getTotal() : BigDecimal.ZERO)
                .description(req.getDescription())
                .dueDate(req.getDueDate())
                .build();
        if (req.getTermId() != null) termRepo.findById(req.getTermId()).ifPresent(inv::setTerm);
        if (req.getFeeItemId() != null) feeItemRepo.findById(req.getFeeItemId()).ifPresent(inv::setFeeItem);
        return invoiceRepo.save(inv);
    }

    // ─── Payments ─────────────────────────────────────────────────────────────

    @GetMapping("/payments")
    public List<Payment> listPayments(
            @RequestParam(required = false) UUID pupilId,
            @RequestParam(required = false) UUID invoiceId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) UUID termId,
            @RequestParam(required = false) UUID academicYearId) {
        List<Payment> base;
        if (invoiceId != null) base = paymentRepo.findByInvoiceIdOrderByPaidOnDesc(invoiceId);
        else if (pupilId != null) base = paymentRepo.findByPupilIdOrderByPaidOnDesc(pupilId);
        else base = paymentRepo.findAllByOrderByPaidOnDesc();
        base = filterByClassAndGrade(base, Payment::getPupil, classId, grade);
        if (termId != null) {
            base = base.stream()
                    .filter(p -> p.getInvoice() != null && p.getInvoice().getTerm() != null
                            && termId.equals(p.getInvoice().getTerm().getId()))
                    .toList();
        }
        if (academicYearId != null) {
            base = base.stream()
                    .filter(p -> p.getInvoice() != null && p.getInvoice().getTerm() != null
                            && p.getInvoice().getTerm().getAcademicYear() != null
                            && academicYearId.equals(p.getInvoice().getTerm().getAcademicYear().getId()))
                    .toList();
        }
        return base;
    }

    @GetMapping("/payments/pending")
    public List<Payment> pendingPayments() {
        return paymentRepo.findByStatusOrderByCreatedAtDesc(PaymentStatus.PENDING);
    }

    @PreAuthorize("@perm.has('fees:collect')")
    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public Payment createPayment(@RequestBody PaymentRequest req) {
        Invoice invoice = null;
        Pupil pupil = null;

        if (req.getInvoiceId() != null) {
            invoice = invoiceRepo.findById(req.getInvoiceId())
                    .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
            pupil = invoice.getPupil();
        } else if (req.getPupilId() != null) {
            pupil = pupilRepo.findById(req.getPupilId())
                    .orElseThrow(() -> new EntityNotFoundException("Pupil not found"));
        }
        if (pupil == null) throw new IllegalArgumentException("Either invoiceId or pupilId is required");

        Payment payment = Payment.builder()
                .receiptNo(generateReceiptNo())
                .pupil(pupil)
                .invoice(invoice)
                .amount(req.getAmount())
                .method(req.getMethod() != null ? req.getMethod() : PaymentMethod.CASH)
                .paidOn(req.getPaidOn() != null ? req.getPaidOn() : LocalDate.now())
                .reference(req.getReference())
                .status(PaymentStatus.CONFIRMED)
                .build();
        if (invoice != null) adjustInvoicePaid(invoice, payment.getAmount());
        return paymentRepo.save(payment);
    }

    // Correcting a wrongly-entered payment: if it had already been applied to an invoice's
    // paid total, the old amount is reversed and the new one re-applied so the invoice
    // balance stays accurate rather than drifting out of sync with the payment history.
    @PreAuthorize("@perm.has('fees:reverse')")
    @PutMapping("/payments/{id}")
    public Payment updatePayment(@PathVariable UUID id, @RequestBody PaymentRequest req) {
        Payment payment = paymentRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Payment not found"));
        boolean wasConfirmed = payment.getStatus() == PaymentStatus.CONFIRMED;
        BigDecimal oldAmount = payment.getAmount();

        if (req.getAmount() != null) payment.setAmount(req.getAmount());
        if (req.getMethod() != null) payment.setMethod(req.getMethod());
        if (req.getPaidOn() != null) payment.setPaidOn(req.getPaidOn());
        if (req.getReference() != null) payment.setReference(req.getReference());

        if (wasConfirmed && payment.getInvoice() != null && req.getAmount() != null) {
            adjustInvoicePaid(payment.getInvoice(), payment.getAmount().subtract(oldAmount));
        }
        audit.log("CORRECT_PAYMENT", "Payment", id.toString(),
                "amount " + oldAmount + " -> " + payment.getAmount() + " (receipt " + payment.getReceiptNo() + ")");
        return paymentRepo.save(payment);
    }

    @PreAuthorize("@perm.has('fees:reverse')")
    @DeleteMapping("/payments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePayment(@PathVariable UUID id) {
        Payment payment = paymentRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Payment not found"));
        audit.log("DELETE_PAYMENT", "Payment", id.toString(),
                "amount " + payment.getAmount() + ", receipt " + payment.getReceiptNo()
                        + (payment.getPupil() != null ? ", pupil " + payment.getPupil().getFullName() : ""));
        if (payment.getStatus() == PaymentStatus.CONFIRMED && payment.getInvoice() != null) {
            adjustInvoicePaid(payment.getInvoice(), payment.getAmount().negate());
        }
        paymentRepo.deleteById(id);
    }

    @PreAuthorize("@perm.has('fees:collect')")
    @PatchMapping("/payments/{id}/confirm")
    public Payment confirmPayment(@PathVariable UUID id) {
        Payment payment = paymentRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Payment not found"));
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Only pending payments can be confirmed");
        }
        if (payment.getInvoice() != null) adjustInvoicePaid(payment.getInvoice(), payment.getAmount());
        payment.setStatus(PaymentStatus.CONFIRMED);
        return paymentRepo.save(payment);
    }

    @PreAuthorize("@perm.has('fees:collect')")
    @PatchMapping("/payments/{id}/reject")
    public Payment rejectPayment(@PathVariable UUID id, @RequestBody(required = false) RejectRequest req) {
        Payment payment = paymentRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Payment not found"));
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new IllegalArgumentException("Only pending payments can be rejected");
        }
        payment.setStatus(PaymentStatus.REJECTED);
        payment.setRejectionReason(req != null ? req.getReason() : null);
        return paymentRepo.save(payment);
    }

    // Grade groups every stream sharing a class name (e.g. "Grade 1 A" + "Grade 1 B"),
    // while classId narrows to one specific class/stream.
    private <T> List<T> filterByClassAndGrade(List<T> rows, java.util.function.Function<T, Pupil> pupilOf, UUID classId, String grade) {
        List<T> result = rows;
        if (classId != null) {
            result = result.stream()
                    .filter(r -> pupilOf.apply(r).getSchoolClass() != null
                            && classId.equals(pupilOf.apply(r).getSchoolClass().getId()))
                    .toList();
        }
        if (grade != null && !grade.isBlank()) {
            result = result.stream()
                    .filter(r -> pupilOf.apply(r).getSchoolClass() != null
                            && grade.equalsIgnoreCase(pupilOf.apply(r).getSchoolClass().getName()))
                    .toList();
        }
        return result;
    }

    // Applies a signed delta to an invoice's paid total — positive to record a payment,
    // negative to reverse one (editing or deleting a previously-confirmed payment) — and
    // recomputes status from the resulting balance rather than assuming it only grows.
    private void adjustInvoicePaid(Invoice invoice, BigDecimal delta) {
        BigDecimal newPaid = invoice.getPaid().add(delta).max(BigDecimal.ZERO);
        invoice.setPaid(newPaid);
        InvoiceStatus status;
        if (newPaid.signum() <= 0) status = InvoiceStatus.UNPAID;
        else if (newPaid.compareTo(invoice.getTotal()) >= 0) status = InvoiceStatus.PAID;
        else status = InvoiceStatus.PARTIAL;
        invoice.setStatus(status);
        invoiceRepo.save(invoice);
    }

    private String generateReceiptNo() {
        return "RCT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    @Data public static class FeeItemRequest {
        private String name;
        private BigDecimal amount;
        private UUID classId, termId;
        private boolean isRecurring = true;
        private String category = "SCHOOL_FEE";
        private LocalDate dueDate;
        // Explicit accessors: Jackson strips the "is" prefix from Lombok's isRecurring()/setRecurring()
        // by default, which would bind this as "recurring" instead of "isRecurring".
        @JsonProperty("isRecurring") public boolean isRecurring() { return isRecurring; }
        @JsonProperty("isRecurring") public void setRecurring(boolean isRecurring) { this.isRecurring = isRecurring; }
    }

    @Data public static class InvoiceRequest {
        private String invoiceNo;
        private UUID pupilId, termId, feeItemId;
        private BigDecimal total;
        private String description;
        private LocalDate dueDate;
    }

    @Data public static class PaymentRequest {
        private UUID pupilId, invoiceId;
        private BigDecimal amount;
        private PaymentMethod method;
        private LocalDate paidOn;
        private String reference;
    }

    @Data public static class RejectRequest {
        private String reason;
    }
}
