package com.brightminds.school.repository;

import com.brightminds.school.entity.FeeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeeItemRepository extends JpaRepository<FeeItem, UUID> {
    List<FeeItem> findByTerm_Id(UUID termId);
    List<FeeItem> findBySchoolClassId(UUID classId);
    Optional<FeeItem> findByCategoryAndNameAndTerm_Id(String category, String name, UUID termId);
    Optional<FeeItem> findByCategoryAndNameAndTermIsNull(String category, String name);

    // TRANSPORT items are excluded: they're legitimately schoolClass=null (a route isn't tied
    // to one class) but must only ever be billed via an actual TransportAssignment
    // (FeeAutoBillingService.billTransportFee) — otherwise every pupil in every class gets
    // auto-charged for a transport route they were never assigned to the moment it's created.
    @Query("SELECT f FROM FeeItem f WHERE f.isRecurring = true AND f.category <> 'TRANSPORT' AND (f.schoolClass IS NULL OR f.schoolClass.id = :classId)")
    List<FeeItem> findRecurringForClass(@Param("classId") UUID classId);
}
