package com.brightminds.school.controller;

import com.brightminds.school.entity.*;
import com.brightminds.school.repository.*;
import com.brightminds.school.service.ClassScopeService;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@RestController @RequestMapping("/canteen") @RequiredArgsConstructor @Tag(name = "Canteen")
@PreAuthorize("@perm.has('canteen:manage')")
public class CanteenController {
    private final CanteenMenuItemRepository menuRepo;
    private final CanteenMealPlanRepository planRepo;
    private final CanteenSaleRepository saleRepo;
    private final CanteenSubscriptionRepository subRepo;
    private final PupilRepository pupilRepo;
    private final TermRepository termRepo;
    private final ClassScopeService scopeService;

    @GetMapping("/menu") public List<CanteenMenuItem> menu() { return menuRepo.findAll(); }
    @PostMapping("/menu") @ResponseStatus(HttpStatus.CREATED) public CanteenMenuItem addItem(@RequestBody MenuItemReq req) {
        return menuRepo.save(CanteenMenuItem.builder().name(req.getName()).category(req.getCategory() != null ? req.getCategory() : "general")
                .description(req.getDescription()).price(req.getPrice()).imageUrl(req.getImageUrl()).isAvailable(true).build()); }
    @PutMapping("/menu/{id}") public CanteenMenuItem updateItem(@PathVariable UUID id, @RequestBody MenuItemReq req) {
        var item = menuRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Item not found"));
        item.setName(req.getName()); item.setCategory(req.getCategory()); item.setDescription(req.getDescription());
        item.setPrice(req.getPrice()); item.setAvailable(req.isAvailable()); return menuRepo.save(item); }
    @DeleteMapping("/menu/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteItem(@PathVariable UUID id) { menuRepo.deleteById(id); }

    @GetMapping("/plans") public List<CanteenMealPlan> plans() { return planRepo.findAll(); }
    @PostMapping("/plans") @ResponseStatus(HttpStatus.CREATED) public CanteenMealPlan createPlan(@RequestBody PlanReq req) {
        return planRepo.save(CanteenMealPlan.builder().name(req.getName()).description(req.getDescription())
                .mealsPerDay(req.getMealsPerDay()).pricePerTerm(req.getPricePerTerm()).isActive(true).build()); }
    @DeleteMapping("/plans/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deletePlan(@PathVariable UUID id) { planRepo.deleteById(id); }

    @GetMapping("/sales") public List<CanteenSale> sales(@RequestParam(required = false) String date, Authentication auth) {
        List<CanteenSale> result = date != null && !date.isBlank()
                ? saleRepo.findByServedOn(java.time.LocalDate.parse(date))
                : saleRepo.findAll();
        Set<UUID> scope = scopeService.restrictedClassIds(auth);
        if (scope == null) return result;
        return result.stream().filter(s -> s.getPupil().getSchoolClass() != null && scope.contains(s.getPupil().getSchoolClass().getId())).toList();
    }
    @PostMapping("/sales") @ResponseStatus(HttpStatus.CREATED) public CanteenSale recordSale(@RequestBody SaleReq req, Authentication auth) {
        if (req.getPupilId() == null) throw new IllegalArgumentException("Pupil is required");
        var pupil = pupilRepo.findById(req.getPupilId())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Pupil not found"));
        scopeService.assertInScope(scopeService.restrictedClassIds(auth), pupil.getSchoolClass() != null ? pupil.getSchoolClass().getId() : null);
        var sale = CanteenSale.builder().itemName(req.getItemName()).quantity(req.getQuantity())
                .unitPrice(req.getUnitPrice()).total(req.getUnitPrice().multiply(BigDecimal.valueOf(req.getQuantity())))
                .paymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "cash").notes(req.getNotes())
                .pupil(pupil).build();
        if (req.getItemId() != null) menuRepo.findById(req.getItemId()).ifPresent(sale::setItem);
        return saleRepo.save(sale);
    }
    @DeleteMapping("/sales/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteSale(@PathVariable UUID id, Authentication auth) {
        CanteenSale sale = saleRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Sale not found"));
        scopeService.assertInScope(scopeService.restrictedClassIds(auth), sale.getPupil().getSchoolClass() != null ? sale.getPupil().getSchoolClass().getId() : null);
        saleRepo.deleteById(id);
    }

    @GetMapping("/subscriptions") public List<CanteenSubscription> subs(@RequestParam(required = false) UUID pupilId, Authentication auth) {
        Set<UUID> scope = scopeService.restrictedClassIds(auth);
        if (pupilId != null) {
            var pupil = pupilRepo.findById(pupilId).orElseThrow(() -> new EntityNotFoundException("Pupil not found"));
            scopeService.assertInScope(scope, pupil.getSchoolClass() != null ? pupil.getSchoolClass().getId() : null);
            return subRepo.findByPupilId(pupilId);
        }
        List<CanteenSubscription> all = subRepo.findAll();
        if (scope == null) return all;
        return all.stream().filter(s -> s.getPupil().getSchoolClass() != null && scope.contains(s.getPupil().getSchoolClass().getId())).toList();
    }
    @PostMapping("/subscriptions") @ResponseStatus(HttpStatus.CREATED) public CanteenSubscription subscribe(@RequestBody SubReq req, Authentication auth) {
        var pupil = pupilRepo.findById(req.getPupilId()).orElseThrow(() -> new EntityNotFoundException("Pupil not found"));
        scopeService.assertInScope(scopeService.restrictedClassIds(auth), pupil.getSchoolClass() != null ? pupil.getSchoolClass().getId() : null);
        var plan = planRepo.findById(req.getPlanId()).orElseThrow(() -> new EntityNotFoundException("Plan not found"));
        var sub = CanteenSubscription.builder().pupil(pupil).plan(plan).status("ACTIVE").build();
        if (req.getTermId() != null) termRepo.findById(req.getTermId()).ifPresent(sub::setTerm);
        return subRepo.save(sub);
    }
    @PatchMapping("/subscriptions/{id}/cancel")
    public CanteenSubscription cancelSub(@PathVariable UUID id, Authentication auth) {
        var sub = subRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Subscription not found"));
        scopeService.assertInScope(scopeService.restrictedClassIds(auth), sub.getPupil().getSchoolClass() != null ? sub.getPupil().getSchoolClass().getId() : null);
        sub.setStatus("CANCELLED");
        return subRepo.save(sub);
    }

    @Data public static class MenuItemReq {
        private String name; private String category; private String description; private BigDecimal price; private String imageUrl;
        private boolean available = true;
        @JsonProperty("isAvailable") public void setAvailable(boolean available) { this.available = available; }
    }
    @Data public static class PlanReq { private String name; private String description; private int mealsPerDay = 1; private BigDecimal pricePerTerm; }
    @Data public static class SaleReq { private UUID itemId; private String itemName; private UUID pupilId; private int quantity = 1; private BigDecimal unitPrice; private String paymentMethod; private String notes; }
    @Data public static class SubReq { private UUID pupilId; private UUID planId; private UUID termId; }
}
