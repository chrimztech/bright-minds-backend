package com.brightminds.school.repository;

import com.brightminds.school.entity.Staff;
import com.brightminds.school.entity.enums.StaffStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StaffRepository extends JpaRepository<Staff, UUID> {
    Optional<Staff> findByStaffNo(String staffNo);
    Optional<Staff> findByUserId(UUID userId);
    boolean existsByStaffNo(String staffNo);
    List<Staff> findByStatus(StaffStatus status);
    List<Staff> findByIsTeacherTrue();

    @Query("SELECT s FROM Staff s WHERE LOWER(s.fullName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(s.staffNo) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<Staff> search(String q, Pageable pageable);

    long countByStatus(StaffStatus status);
}
