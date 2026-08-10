package com.brightminds.school.repository;

import com.brightminds.school.entity.CanteenSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CanteenSubscriptionRepository extends JpaRepository<CanteenSubscription, UUID> {
    List<CanteenSubscription> findByPupilId(UUID pupilId);
}
