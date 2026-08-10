package com.brightminds.school.repository;

import com.brightminds.school.entity.InventoryTxn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InventoryTxnRepository extends JpaRepository<InventoryTxn, UUID> {
    List<InventoryTxn> findByItemId(UUID itemId);
}
