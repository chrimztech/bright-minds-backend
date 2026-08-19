package com.brightminds.school.repository;

import com.brightminds.school.entity.TransportPickupPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TransportPickupPointRepository extends JpaRepository<TransportPickupPoint, UUID> {
    List<TransportPickupPoint> findByRouteId(UUID routeId);
}
