package com.brightminds.school.repository;

import com.brightminds.school.entity.Homework;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface HomeworkRepository extends JpaRepository<Homework, UUID> {
    List<Homework> findBySchoolClassIdOrderByDueDateDesc(UUID classId);
    List<Homework> findAllByOrderByCreatedAtDesc();
}
