package com.brightminds.school.repository;

import com.brightminds.school.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {
    List<PurchaseOrder> findAllByOrderByOrderDateDesc();
    boolean existsByPoNo(String poNo);
}
