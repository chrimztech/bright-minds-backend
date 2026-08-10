package com.brightminds.school.repository;

import com.brightminds.school.entity.CanteenSale;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CanteenSaleRepository extends JpaRepository<CanteenSale, UUID> {
    List<CanteenSale> findByServedOn(LocalDate servedOn);
}
