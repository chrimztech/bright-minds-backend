package com.brightminds.school.repository;

import com.brightminds.school.entity.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface InquiryRepository extends JpaRepository<Inquiry, UUID> {
    List<Inquiry> findAllByOrderByCreatedAtDesc();
}
