package com.brightminds.school.repository;

import com.brightminds.school.entity.CanteenMealPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CanteenMealPlanRepository extends JpaRepository<CanteenMealPlan, UUID> {}
