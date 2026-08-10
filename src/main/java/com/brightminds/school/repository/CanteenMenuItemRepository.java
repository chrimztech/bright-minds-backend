package com.brightminds.school.repository;

import com.brightminds.school.entity.CanteenMenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CanteenMenuItemRepository extends JpaRepository<CanteenMenuItem, UUID> {}
