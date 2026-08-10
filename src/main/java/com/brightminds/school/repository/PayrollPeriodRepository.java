package com.brightminds.school.repository;

import com.brightminds.school.entity.PayrollPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, UUID> {
    List<PayrollPeriod> findAllByOrderByPeriodStartDesc();
}
