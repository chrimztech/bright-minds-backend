package com.brightminds.school.repository;

import com.brightminds.school.entity.FeeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface FeeItemRepository extends JpaRepository<FeeItem, UUID> {
    List<FeeItem> findByTerm_Id(UUID termId);
    List<FeeItem> findBySchoolClassId(UUID classId);

    @Query("SELECT f FROM FeeItem f WHERE f.isRecurring = true AND (f.schoolClass IS NULL OR f.schoolClass.id = :classId)")
    List<FeeItem> findRecurringForClass(@Param("classId") UUID classId);
}
