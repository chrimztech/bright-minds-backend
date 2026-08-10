package com.brightminds.school.repository;

import com.brightminds.school.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByPupilId(UUID pupilId);
    List<Document> findByStaffId(UUID staffId);
}
