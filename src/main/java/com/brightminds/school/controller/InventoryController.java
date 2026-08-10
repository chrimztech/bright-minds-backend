package com.brightminds.school.controller;

import com.brightminds.school.entity.InventoryItem;
import com.brightminds.school.entity.InventoryTxn;
import com.brightminds.school.entity.enums.InvDirection;
import com.brightminds.school.repository.InventoryItemRepository;
import com.brightminds.school.repository.InventoryTxnRepository;
import com.brightminds.school.repository.SupplierRepository;
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

@RestController @RequestMapping("/inventory") @RequiredArgsConstructor @Tag(name = "Inventory")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
public class InventoryController {
    private final InventoryItemRepository itemRepo;
    private final InventoryTxnRepository txnRepo;
    private final SupplierRepository supplierRepo;

    @GetMapping("/items") public List<InventoryItem> list() { return itemRepo.findAll(); }
    @GetMapping("/items/low-stock") public List<InventoryItem> lowStock() { return itemRepo.findLowStock(); }
    @PostMapping("/items") @ResponseStatus(HttpStatus.CREATED) public InventoryItem create(@RequestBody ItemRequest req) {
        var item = InventoryItem.builder().name(req.getName()).sku(req.getSku()).category(req.getCategory())
                .unit(req.getUnit()).location(req.getLocation()).quantity(req.getQuantity())
                .reorderLevel(req.getReorderLevel()).unitCost(req.getUnitCost()).build();
        if (req.getSupplierId() != null) supplierRepo.findById(req.getSupplierId()).ifPresent(item::setSupplier);
        return itemRepo.save(item);
    }
    @PutMapping("/items/{id}") public InventoryItem update(@PathVariable UUID id, @RequestBody ItemRequest req) {
        var item = itemRepo.findById(id).orElseThrow(() -> new EntityNotFoundException("Item not found"));
        item.setName(req.getName()); item.setSku(req.getSku()); item.setCategory(req.getCategory());
        item.setUnit(req.getUnit()); item.setLocation(req.getLocation());
        item.setReorderLevel(req.getReorderLevel()); item.setUnitCost(req.getUnitCost());
        return itemRepo.save(item);
    }
    @DeleteMapping("/items/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable UUID id) { itemRepo.deleteById(id); }

    @GetMapping("/transactions") public List<InventoryTxn> listTxns() { return txnRepo.findAll(); }
    @PostMapping("/transactions") @ResponseStatus(HttpStatus.CREATED) public InventoryTxn createTxn(@RequestBody TxnRequest req) {
        var item = itemRepo.findById(req.getItemId()).orElseThrow(() -> new EntityNotFoundException("Item not found"));
        if (req.getDirection() == InvDirection.IN || req.getDirection() == InvDirection.ADJUST) {
            item.setQuantity(item.getQuantity() + req.getQuantity());
        } else {
            item.setQuantity(Math.max(0, item.getQuantity() - req.getQuantity()));
        }
        itemRepo.save(item);
        return txnRepo.save(InventoryTxn.builder().item(item).direction(req.getDirection())
                .quantity(req.getQuantity()).reason(req.getReason()).reference(req.getReference()).build());
    }

    @Data public static class ItemRequest {
        private String name; private String sku; private String category; private String unit;
        private String location; private int quantity; private Integer reorderLevel;
        private BigDecimal unitCost; private UUID supplierId;
    }
    @Data public static class TxnRequest {
        private UUID itemId; private InvDirection direction; private int quantity;
        private String reason; private String reference;
    }
}
