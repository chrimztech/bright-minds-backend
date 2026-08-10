package com.brightminds.school.repository;

import com.brightminds.school.entity.PtcSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PtcSessionRepository extends JpaRepository<PtcSession, UUID> {
    List<PtcSession> findAllByOrderBySessionDateDesc();
}
