package com.brightminds.school.controller;

import com.brightminds.school.entity.Expense;
import com.brightminds.school.repository.ExpenseRepository;
import com.brightminds.school.service.AuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/accounts") @RequiredArgsConstructor @Tag(name = "Accounts / Expenses")
@PreAuthorize("@perm.has('accounts:manage')")
public class AccountsController {
    private final ExpenseRepository repo;
    private final AuditService audit;

    @GetMapping("/expenses") public List<Expense> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (from != null && to != null) return repo.findBySpentOnBetween(from, to);
        return repo.findAllByOrderBySpentOnDesc();
    }
    @PostMapping("/expenses") @ResponseStatus(HttpStatus.CREATED) public Expense create(@RequestBody ExpenseReq req) {
        return repo.save(Expense.builder().category(req.getCategory()).amount(req.getAmount())
                .payee(req.getPayee()).description(req.getDescription()).paymentMethod(req.getPaymentMethod())
                .refNo(req.getRefNo()).spentOn(req.getSpentOn() != null ? req.getSpentOn() : LocalDate.now()).build()); }
    @PreAuthorize("@perm.has('accounts:reverse')")
    @PutMapping("/expenses/{id}") public Expense update(@PathVariable UUID id, @RequestBody ExpenseReq req) {
        var e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Expense not found"));
        BigDecimal oldAmount = e.getAmount();
        e.setCategory(req.getCategory()); e.setAmount(req.getAmount()); e.setPayee(req.getPayee());
        e.setDescription(req.getDescription()); e.setPaymentMethod(req.getPaymentMethod());
        e.setRefNo(req.getRefNo()); if (req.getSpentOn() != null) e.setSpentOn(req.getSpentOn());
        audit.log("CORRECT_EXPENSE", "Expense", id.toString(), "amount " + oldAmount + " -> " + e.getAmount());
        return repo.save(e); }
    @PreAuthorize("@perm.has('accounts:reverse')")
    @DeleteMapping("/expenses/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) {
        var e = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Expense not found"));
        audit.log("DELETE_EXPENSE", "Expense", id.toString(),
                "amount " + e.getAmount() + (e.getPayee() != null ? ", payee " + e.getPayee() : ""));
        repo.deleteById(id);
    }

    @Data public static class ExpenseReq {
        private String category; private BigDecimal amount; private String payee;
        private String description; private String paymentMethod; private String refNo; private LocalDate spentOn;
    }
}
