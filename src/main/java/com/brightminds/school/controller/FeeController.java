package com.brightminds.school.controller;

import com.brightminds.school.entity.*;
import com.brightminds.school.entity.enums.InvoiceStatus;
import com.brightminds.school.entity.enums.PaymentMethod;
import com.brightminds.school.repository.*;
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
@PreAuthorize("hasAnyRole('SUPER_ADMIN','HEAD_TEACHER','ADMIN','ACCOUNTANT')")
public class FeeController {

    private final FeeItemRepository feeItemRepo;
    private final InvoiceRepository invoiceRepo;
    private final PaymentRepository paymentRepo;
    private final PupilRepository pupilRepo;
    private final TermRepository termRepo;
    private final SchoolClassRepository classRepo;

    // ─── Fee Items ────────────────────────────────────────────────────────────

    @GetMapping("/items")
    public List<FeeItem> listItems() { return feeItemRepo.findAll(); }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public FeeItem createItem(@RequestBody FeeItemRequest req) {
        FeeItem item = FeeItem.builder()
                .name(req.getName())
                .amount(req.getAmount() != null ? req.getAmount() : BigDecimal.ZERO)
                .isRecurring(req.isRecurring())
                .build();
        item.setCategory(req.getCategory() != null ? req.getCategory() : "SCHOOL_FEE");
        if (req.getClassId() != null) classRepo.findById(req.getClassId()).ifPresent(item::setSchoolClass);
        if (req.getTermId() != null) termRepo.findById(req.getTermId()).ifPresent(item::setTerm);
        return feeItemRepo.save(item);
    }

    @PutMapping("/items/{id}")
    public FeeItem updateItem(@PathVariable UUID id, @RequestBody FeeItemRequest req) {
        FeeItem item = feeItemRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Fee item not found"));
        if (req.getName() != null) item.setName(req.getName());
        if (req.getAmount() != null) item.setAmount(req.getAmount());
        if (req.getCategory() != null) item.setCategory(req.getCategory());
        return feeItemRepo.save(item);
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable UUID id) { feeItemRepo.deleteById(id); }

    // ─── Invoices ─────────────────────────────────────────────────────────────

    @GetMapping("/invoices")
    public List<Invoice> listInvoices(
            @RequestParam(required = false) UUID pupilId,
            @RequestParam(required = false) UUID termId) {
        if (pupilId != null) return invoiceRepo.findByPupilIdOrderByCreatedAtDesc(pupilId);
        if (termId != null) return invoiceRepo.findByTerm_IdOrderByCreatedAtDesc(termId);
        return invoiceRepo.findAllByOrderByCreatedAtDesc();
    }

    @GetMapping("/invoices/{id}")
    public Invoice getInvoice(@PathVariable UUID id) {
        return invoiceRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
    }

    @PostMapping("/invoices")
    @ResponseStatus(HttpStatus.CREATED)
    public Invoice createInvoice(@RequestBody InvoiceRequest req) {
        return saveInvoice(req);
    }

    @PostMapping("/invoices/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Invoice> bulkCreateInvoices(@RequestBody List<InvoiceRequest> reqs) {
        return reqs.stream().map(this::saveInvoice).toList();
    }

    private Invoice saveInvoice(InvoiceRequest req) {
        Pupil pupil = pupilRepo.findById(req.getPupilId())
                .orElseThrow(() -> new EntityNotFoundException("Pupil not found: " + req.getPupilId()));
        String invNo = req.getInvoiceNo() != null && !req.getInvoiceNo().isBlank()
                ? req.getInvoiceNo()
                : "INV-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                  + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        Invoice inv = Invoice.builder()
                .invoiceNo(invNo)
                .pupil(pupil)
                .total(req.getTotal() != null ? req.getTotal() : BigDecimal.ZERO)
                .description(req.getDescription())
                .dueDate(req.getDueDate())
                .build();
        if (req.getTermId() != null) termRepo.findById(req.getTermId()).ifPresent(inv::setTerm);
        return invoiceRepo.save(inv);
    }

    // ─── Payments ─────────────────────────────────────────────────────────────

    @GetMapping("/payments")
    public List<Payment> listPayments(
            @RequestParam(required = false) UUID pupilId,
            @RequestParam(required = false) UUID invoiceId) {
        if (invoiceId != null) return paymentRepo.findByInvoiceIdOrderByPaidOnDesc(invoiceId);
        if (pupilId != null) return paymentRepo.findByPupilIdOrderByPaidOnDesc(pupilId);
        return paymentRepo.findAllByOrderByPaidOnDesc();
    }

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public Payment createPayment(@RequestBody PaymentRequest req) {
        Invoice invoice = null;
        Pupil pupil = null;

        if (req.getInvoiceId() != null) {
            invoice = invoiceRepo.findById(req.getInvoiceId())
                    .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
            pupil = invoice.getPupil();
            BigDecimal newPaid = invoice.getPaid().add(req.getAmount());
            invoice.setPaid(newPaid);
            invoice.setStatus(newPaid.compareTo(invoice.getTotal()) >= 0 ? InvoiceStatus.PAID : InvoiceStatus.PARTIAL);
            invoiceRepo.save(invoice);
        } else if (req.getPupilId() != null) {
            pupil = pupilRepo.findById(req.getPupilId())
                    .orElseThrow(() -> new EntityNotFoundException("Pupil not found"));
        }

        if (pupil == null) throw new IllegalArgumentException("Either invoiceId or pupilId is required");

        String receiptNo = "RCT-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        Payment payment = Payment.builder()
                .receiptNo(receiptNo)
                .pupil(pupil)
                .invoice(invoice)
                .amount(req.getAmount())
                .method(req.getMethod() != null ? req.getMethod() : PaymentMethod.CASH)
                .paidOn(req.getPaidOn() != null ? req.getPaidOn() : LocalDate.now())
                .reference(req.getReference())
                .build();
        return paymentRepo.save(payment);
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    @Data public static class FeeItemRequest {
        private String name;
        private BigDecimal amount;
        private UUID classId, termId;
        private boolean isRecurring = true;
        private String category = "SCHOOL_FEE";
        // Explicit accessors: Jackson strips the "is" prefix from Lombok's isRecurring()/setRecurring()
        // by default, which would bind this as "recurring" instead of "isRecurring".
        @JsonProperty("isRecurring") public boolean isRecurring() { return isRecurring; }
        @JsonProperty("isRecurring") public void setRecurring(boolean isRecurring) { this.isRecurring = isRecurring; }
    }

    @Data public static class InvoiceRequest {
        private String invoiceNo;
        private UUID pupilId, termId;
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
}
