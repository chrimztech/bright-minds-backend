package com.brightminds.school.controller;

import com.brightminds.school.entity.FeeItem;
import com.brightminds.school.entity.Invoice;
import com.brightminds.school.entity.Pupil;
import com.brightminds.school.repository.FeeItemRepository;
import com.brightminds.school.repository.InvoiceRepository;
import com.brightminds.school.repository.PupilRepository;
import com.brightminds.school.service.ClassScopeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// School uniform / attire catalog and sales (Operations). A "sale" is simply an Invoice
// tagged with a UNIFORM-category FeeItem, reusing the existing invoice/payment plumbing
// (Fees & Payments, Accounts reporting) rather than building a parallel accounting path.
@RestController
@RequestMapping("/uniform")
@RequiredArgsConstructor
@Tag(name = "Uniform")
public class UniformController {

    private static final String CATEGORY = "UNIFORM";

    private final FeeItemRepository feeItemRepo;
    private final InvoiceRepository invoiceRepo;
    private final PupilRepository pupilRepo;
    private final ClassScopeService scopeService;

    @PreAuthorize("@perm.has('uniform:view') or @perm.has('uniform:manage')")
    @GetMapping("/items")
    public List<FeeItem> items() {
        return feeItemRepo.findByCategoryOrderByNameAsc(CATEGORY);
    }

    @PreAuthorize("@perm.has('uniform:manage')")
    @PostMapping("/items")
    @ResponseStatus(HttpStatus.CREATED)
    public FeeItem addItem(@RequestBody ItemRequest req) {
        FeeItem item = FeeItem.builder()
                .name(req.getName())
                .amount(req.getPrice() != null ? req.getPrice() : BigDecimal.ZERO)
                .isRecurring(false)
                .build();
        item.setCategory(CATEGORY);
        return feeItemRepo.save(item);
    }

    @PreAuthorize("@perm.has('uniform:manage')")
    @PutMapping("/items/{id}")
    public FeeItem updateItem(@PathVariable UUID id, @RequestBody ItemRequest req) {
        FeeItem item = feeItemRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Uniform item not found"));
        if (req.getName() != null) item.setName(req.getName());
        if (req.getPrice() != null) item.setAmount(req.getPrice());
        return feeItemRepo.save(item);
    }

    @PreAuthorize("@perm.has('uniform:manage')")
    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteItem(@PathVariable UUID id) {
        feeItemRepo.deleteById(id);
    }

    // Sale history — a UNIFORM-tagged invoice. Scoped the same way Canteen sales are: a
    // class-restricted teacher only sees sales for pupils in the class(es) they teach.
    @PreAuthorize("@perm.has('uniform:view') or @perm.has('uniform:manage')")
    @GetMapping("/sales")
    public List<Invoice> sales(Authentication auth) {
        List<Invoice> all = invoiceRepo.findByFeeItem_CategoryOrderByCreatedAtDesc(CATEGORY);
        Set<UUID> scope = scopeService.restrictedClassIds(auth);
        if (scope == null) return all;
        return all.stream()
                .filter(inv -> inv.getPupil().getSchoolClass() != null
                        && scope.contains(inv.getPupil().getSchoolClass().getId()))
                .toList();
    }

    @PreAuthorize("@perm.has('uniform:manage')")
    @PostMapping("/sales")
    @ResponseStatus(HttpStatus.CREATED)
    public Invoice sell(@RequestBody SaleRequest req, Authentication auth) {
        Pupil pupil = pupilRepo.findById(req.getPupilId())
                .orElseThrow(() -> new EntityNotFoundException("Pupil not found"));
        scopeService.assertInScope(scopeService.restrictedClassIds(auth),
                pupil.getSchoolClass() != null ? pupil.getSchoolClass().getId() : null);
        FeeItem item = feeItemRepo.findById(req.getItemId())
                .orElseThrow(() -> new EntityNotFoundException("Uniform item not found"));
        int quantity = req.getQuantity() > 0 ? req.getQuantity() : 1;
        BigDecimal total = item.getAmount().multiply(BigDecimal.valueOf(quantity));
        String description = item.getName() + (quantity > 1 ? " x" + quantity : "")
                + (req.getNotes() != null && !req.getNotes().isBlank() ? " — " + req.getNotes().trim() : "");
        Invoice invoice = Invoice.builder()
                .invoiceNo(nextInvoiceNo())
                .pupil(pupil)
                .feeItem(item)
                .total(total)
                .description(description)
                .dueDate(req.getDueDate())
                .build();
        return invoiceRepo.save(invoice);
    }

    private String nextInvoiceNo() {
        return "UNI-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
                + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
    }

    @Data
    public static class ItemRequest {
        private String name;
        private BigDecimal price;
    }

    @Data
    public static class SaleRequest {
        private UUID pupilId;
        private UUID itemId;
        private int quantity = 1;
        private String notes;
        private LocalDate dueDate;
    }
}
