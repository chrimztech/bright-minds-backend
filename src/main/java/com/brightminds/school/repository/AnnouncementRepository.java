package com.brightminds.school.repository;

import com.brightminds.school.entity.Announcement;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {
    List<Announcement> findAllByOrderByCreatedAtDesc();
    List<Announcement> findTop5ByOrderByCreatedAtDesc();
}
