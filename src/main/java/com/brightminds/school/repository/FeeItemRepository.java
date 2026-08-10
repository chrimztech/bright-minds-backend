package com.brightminds.school.repository;

import com.brightminds.school.entity.FeeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface FeeItemRepository extends JpaRepository<FeeItem, UUID> {
    List<FeeItem> findByTerm_Id(UUID termId);
    List<FeeItem> findBySchoolClassId(UUID classId);
}
