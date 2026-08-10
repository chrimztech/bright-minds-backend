package com.brightminds.school.repository;

import com.brightminds.school.entity.PupilPromotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PupilPromotionRepository extends JpaRepository<PupilPromotion, UUID> {
    List<PupilPromotion> findByPupilIdOrderByPromotedOnDesc(UUID pupilId);
}
