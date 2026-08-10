package com.brightminds.school.controller;

import com.brightminds.school.entity.PurchaseOrder;
import com.brightminds.school.entity.Supplier;
import com.brightminds.school.entity.enums.PoStatus;
import com.brightminds.school.repository.PurchaseOrderRepository;
import com.brightminds.school.repository.SupplierRepository;
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

@RestController @RequestMapping("/procurement") @RequiredArgsConstructor @Tag(name = "Procurement")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
public class ProcurementController {
    private final PurchaseOrderRepository poRepo;
    private final SupplierRepository supplierRepo;

    @GetMapping("/suppliers") public List<Supplier> suppliers() { return supplierRepo.findAll(); }
    @PostMapping("/suppliers") @ResponseStatus(HttpStatus.CREATED) public Supplier createSupplier(@RequestBody SupplierReq req) {
        return supplierRepo.save(Supplier.builder().name(req.getName()).contactPerson(req.getContactPerson())
                .phone(req.getPhone()).email(req.getEmail()).address(req.getAddress()).taxNo(req.getTaxNo()).notes(req.getNotes()).build()); }
    @PutMapping("/suppliers/{id}") public Supplier updateSupplier(@PathVariable UUID id, @RequestBody SupplierReq req) {
        var s = supplierRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Supplier not found"));
        s.setName(req.getName()); s.setContactPerson(req.getContactPerson()); s.setPhone(req.getPhone());
        s.setEmail(req.getEmail()); s.setAddress(req.getAddress()); s.setTaxNo(req.getTaxNo()); s.setNotes(req.getNotes());
        return supplierRepo.save(s); }
    @DeleteMapping("/suppliers/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteSupplier(@PathVariable UUID id) { supplierRepo.deleteById(id); }

    @GetMapping("/orders") public List<PurchaseOrder> orders() { return poRepo.findAllByOrderByOrderDateDesc(); }
    @PostMapping("/orders") @ResponseStatus(HttpStatus.CREATED) public PurchaseOrder createOrder(@RequestBody OrderReq req) {
        var po = PurchaseOrder.builder().poNo(req.getPoNo()).orderDate(req.getOrderDate() != null ? req.getOrderDate() : LocalDate.now())
                .total(req.getTotal() != null ? req.getTotal() : BigDecimal.ZERO).notes(req.getNotes()).build();
        if (req.getSupplierId() != null) supplierRepo.findById(req.getSupplierId()).ifPresent(po::setSupplier);
        return poRepo.save(po); }
    @PatchMapping("/orders/{id}/status") public PurchaseOrder updateStatus(@PathVariable UUID id, @RequestParam String status) {
        var po = poRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        po.setStatus(PoStatus.valueOf(status.toUpperCase())); return poRepo.save(po); }
    @DeleteMapping("/orders/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteOrder(@PathVariable UUID id) { poRepo.deleteById(id); }

    @Data public static class SupplierReq { private String name; private String contactPerson; private String phone; private String email; private String address; private String taxNo; private String notes; }
    @Data public static class OrderReq { private String poNo; private UUID supplierId; private LocalDate orderDate; private BigDecimal total; private String notes; }
}
