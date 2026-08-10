package com.brightminds.school.repository;

import com.brightminds.school.entity.InventoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.UUID;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, UUID> {
    @Query("SELECT i FROM InventoryItem i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<InventoryItem> search(String q, Pageable pageable);

    @Query("SELECT i FROM InventoryItem i WHERE i.quantity <= i.reorderLevel AND i.reorderLevel IS NOT NULL")
    java.util.List<InventoryItem> findLowStock();
}
