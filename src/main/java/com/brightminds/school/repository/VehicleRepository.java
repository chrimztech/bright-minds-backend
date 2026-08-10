package com.brightminds.school.repository;

import com.brightminds.school.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {}
