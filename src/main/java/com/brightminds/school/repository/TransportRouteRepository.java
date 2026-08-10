package com.brightminds.school.repository;

import com.brightminds.school.entity.TransportRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TransportRouteRepository extends JpaRepository<TransportRoute, UUID> {}
