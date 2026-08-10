package com.brightminds.school.controller;

import com.brightminds.school.entity.*;
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
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/canteen") @RequiredArgsConstructor @Tag(name = "Canteen")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','HEAD_TEACHER','ADMIN','ACCOUNTANT')")
public class CanteenController {
    private final CanteenMenuItemRepository menuRepo;
    private final CanteenMealPlanRepository planRepo;
    private final CanteenSaleRepository saleRepo;
    private final CanteenSubscriptionRepository subRepo;
    private final PupilRepository pupilRepo;
    private final TermRepository termRepo;

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

    @GetMapping("/sales") public List<CanteenSale> sales(@RequestParam(required = false) String date) {
        if (date != null && !date.isBlank()) return saleRepo.findByServedOn(java.time.LocalDate.parse(date));
        return saleRepo.findAll(); }
    @PostMapping("/sales") @ResponseStatus(HttpStatus.CREATED) public CanteenSale recordSale(@RequestBody SaleReq req) {
        var sale = CanteenSale.builder().itemName(req.getItemName()).quantity(req.getQuantity())
                .unitPrice(req.getUnitPrice()).total(req.getUnitPrice().multiply(BigDecimal.valueOf(req.getQuantity())))
                .paymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "cash").notes(req.getNotes()).build();
        if (req.getItemId() != null) menuRepo.findById(req.getItemId()).ifPresent(sale::setItem);
        if (req.getPupilId() != null) pupilRepo.findById(req.getPupilId()).ifPresent(sale::setPupil);
        return saleRepo.save(sale);
    }
    @DeleteMapping("/sales/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteSale(@PathVariable UUID id) { saleRepo.deleteById(id); }

    @GetMapping("/subscriptions") public List<CanteenSubscription> subs(@RequestParam(required = false) UUID pupilId) {
        if (pupilId != null) return subRepo.findByPupilId(pupilId);
        return subRepo.findAll();
    }
    @PostMapping("/subscriptions") @ResponseStatus(HttpStatus.CREATED) public CanteenSubscription subscribe(@RequestBody SubReq req) {
        var pupil = pupilRepo.findById(req.getPupilId()).orElseThrow(() -> new EntityNotFoundException("Pupil not found"));
        var plan = planRepo.findById(req.getPlanId()).orElseThrow(() -> new EntityNotFoundException("Plan not found"));
        var sub = CanteenSubscription.builder().pupil(pupil).plan(plan).status("ACTIVE").build();
        if (req.getTermId() != null) termRepo.findById(req.getTermId()).ifPresent(sub::setTerm);
        return subRepo.save(sub);
    }
    @PatchMapping("/subscriptions/{id}/cancel")
    public CanteenSubscription cancelSub(@PathVariable UUID id) {
        var sub = subRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Subscription not found"));
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
