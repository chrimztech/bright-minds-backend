package com.brightminds.school.controller;

import com.brightminds.school.entity.InventoryItem;
import com.brightminds.school.entity.InventoryTxn;
import com.brightminds.school.entity.PurchaseOrder;
import com.brightminds.school.entity.PurchaseOrderItem;
import com.brightminds.school.entity.Supplier;
import com.brightminds.school.entity.enums.InvDirection;
import com.brightminds.school.entity.enums.PoStatus;
import com.brightminds.school.repository.InventoryItemRepository;
import com.brightminds.school.repository.InventoryTxnRepository;
import com.brightminds.school.repository.PurchaseOrderItemRepository;
import com.brightminds.school.repository.PurchaseOrderRepository;
import com.brightminds.school.repository.SupplierRepository;
import com.brightminds.school.service.AuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController @RequestMapping("/procurement") @RequiredArgsConstructor @Tag(name = "Procurement")
@PreAuthorize("@perm.has('procurement:manage')")
public class ProcurementController {
    private final PurchaseOrderRepository poRepo;
    private final PurchaseOrderItemRepository poItemRepo;
    private final SupplierRepository supplierRepo;
    private final InventoryItemRepository inventoryRepo;
    private final InventoryTxnRepository txnRepo;
    private final AuditService audit;

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
    @GetMapping("/orders/{id}/items") public List<PurchaseOrderItem> orderItems(@PathVariable UUID id) { return poItemRepo.findByPurchaseOrderId(id); }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PurchaseOrder createOrder(@RequestBody OrderReq req) {
        // The frontend's "New PO" form has no poNo field, so req.getPoNo() was always null —
        // the "PO NO" table column showed blank and the delete-confirm dialog literally read
        // "Delete null?". Auto-generate like Admission/Pupil numbers.
        String poNo = req.getPoNo() == null || req.getPoNo().isBlank() ? generatePoNo() : req.getPoNo().trim();
        var po = PurchaseOrder.builder().poNo(poNo).orderDate(req.getOrderDate() != null ? req.getOrderDate() : LocalDate.now())
                .notes(req.getNotes()).build();
        if (req.getSupplierId() != null) supplierRepo.findById(req.getSupplierId()).ifPresent(po::setSupplier);

        // Line items are what make "receive" able to credit real stock — if none are given
        // (e.g. a non-stock purchase), total falls back to the manually entered amount so
        // the PO still works as a plain expense-tracking record like before.
        BigDecimal computedTotal = BigDecimal.ZERO;
        if (req.getItems() != null && !req.getItems().isEmpty()) {
            po.setTotal(BigDecimal.ZERO);
            var saved = poRepo.save(po);
            for (OrderItemReq itemReq : req.getItems()) {
                InventoryItem item = inventoryRepo.findById(itemReq.getItemId())
                        .orElseThrow(() -> new EntityNotFoundException("Inventory item not found: " + itemReq.getItemId()));
                BigDecimal unitCost = itemReq.getUnitCost() != null ? itemReq.getUnitCost() : (item.getUnitCost() != null ? item.getUnitCost() : BigDecimal.ZERO);
                poItemRepo.save(PurchaseOrderItem.builder().purchaseOrder(saved).item(item)
                        .quantity(itemReq.getQuantity()).unitCost(unitCost).build());
                computedTotal = computedTotal.add(unitCost.multiply(BigDecimal.valueOf(itemReq.getQuantity())));
            }
            saved.setTotal(computedTotal);
            return poRepo.save(saved);
        }
        po.setTotal(req.getTotal() != null ? req.getTotal() : BigDecimal.ZERO);
        return poRepo.save(po);
    }

    @PatchMapping("/orders/{id}/status")
    public PurchaseOrder updateStatus(@PathVariable UUID id, @RequestParam String status) {
        var po = poRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        PoStatus newStatus = PoStatus.valueOf(status.toUpperCase());
        // RECEIVED must go through /receive, which is the only path that actually credits
        // inventory stock — otherwise this endpoint would let the status say "received"
        // while stock silently stays wrong, the exact bug this was built to close.
        if (newStatus == PoStatus.RECEIVED) {
            throw new IllegalArgumentException("Use POST /procurement/orders/{id}/receive to mark an order received — it also credits inventory stock.");
        }
        po.setStatus(newStatus);
        return poRepo.save(po);
    }

    // Credits each line item's quantity to its InventoryItem's stock via the same
    // inventory_txns mechanism manual stock adjustments use (InventoryController.createTxn),
    // so received stock shows up identically in transaction history either way.
    @PostMapping("/orders/{id}/receive")
    @Transactional
    public PurchaseOrder receive(@PathVariable UUID id) {
        var po = poRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Order not found"));
        if (po.getReceivedAt() != null) throw new IllegalArgumentException("This order has already been received");
        if (po.getStatus() == PoStatus.CANCELLED) throw new IllegalArgumentException("A cancelled order cannot be received");

        List<PurchaseOrderItem> items = poItemRepo.findByPurchaseOrderId(id);
        if (items.isEmpty()) throw new IllegalArgumentException("This order has no line items to receive — add items when creating a PO to enable receiving");

        for (PurchaseOrderItem line : items) {
            InventoryItem item = line.getItem();
            item.setQuantity(item.getQuantity() + line.getQuantity());
            inventoryRepo.save(item);
            txnRepo.save(InventoryTxn.builder().item(item).direction(InvDirection.IN)
                    .quantity(line.getQuantity()).reason("Received purchase order").reference(po.getPoNo()).build());
        }

        po.setStatus(PoStatus.RECEIVED);
        po.setReceivedAt(Instant.now());
        PurchaseOrder saved = poRepo.save(po);
        audit.log("RECEIVE_PURCHASE_ORDER", "PurchaseOrder", id.toString(),
                items.size() + " line item(s) credited to stock (PO " + po.getPoNo() + ")");
        return saved;
    }

    @DeleteMapping("/orders/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteOrder(@PathVariable UUID id) { poRepo.deleteById(id); }

    private String generatePoNo() {
        String candidate;
        do {
            candidate = "PO-%d-%s".formatted(
                    LocalDate.now().getYear(),
                    UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        } while (poRepo.existsByPoNo(candidate));
        return candidate;
    }

    @Data public static class SupplierReq { private String name; private String contactPerson; private String phone; private String email; private String address; private String taxNo; private String notes; }
    @Data public static class OrderReq { private String poNo; private UUID supplierId; private LocalDate orderDate; private BigDecimal total; private String notes; private List<OrderItemReq> items; }
    @Data public static class OrderItemReq { private UUID itemId; private int quantity; private BigDecimal unitCost; }
}
