package com.brightminds.school.repository;

import com.brightminds.school.entity.Admission;
import com.brightminds.school.entity.enums.AdmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface AdmissionRepository extends JpaRepository<Admission, UUID> {
    List<Admission> findByStatus(AdmissionStatus status);

    @Query("SELECT a FROM Admission a WHERE LOWER(a.fullName) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<Admission> search(String q, Pageable pageable);
}
