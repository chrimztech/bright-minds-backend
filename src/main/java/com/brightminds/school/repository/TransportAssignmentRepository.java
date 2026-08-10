package com.brightminds.school.repository;

import com.brightminds.school.entity.TransportAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransportAssignmentRepository extends JpaRepository<TransportAssignment, UUID> {
    List<TransportAssignment> findByRouteId(UUID routeId);
    Optional<TransportAssignment> findByPupilIdAndActiveTrue(UUID pupilId);
}
