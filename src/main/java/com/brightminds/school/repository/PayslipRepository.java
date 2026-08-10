package com.brightminds.school.repository;

import com.brightminds.school.entity.Payslip;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PayslipRepository extends JpaRepository<Payslip, UUID> {
    List<Payslip> findByPeriodId(UUID periodId);
    List<Payslip> findByStaffId(UUID staffId);
    boolean existsByStaffIdAndPeriodId(UUID staffId, UUID periodId);
}
