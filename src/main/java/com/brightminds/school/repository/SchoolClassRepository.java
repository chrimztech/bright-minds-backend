package com.brightminds.school.repository;

import com.brightminds.school.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {
    List<SchoolClass> findAllByOrderByLevelOrderAsc();
    boolean existsByName(String name);
    List<SchoolClass> findByClassTeacherUserId(UUID userId);
}
