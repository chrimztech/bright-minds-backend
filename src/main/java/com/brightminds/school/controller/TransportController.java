package com.brightminds.school.controller;

import com.brightminds.school.entity.TransportAssignment;
import com.brightminds.school.entity.TransportRoute;
import com.brightminds.school.entity.Vehicle;
import com.brightminds.school.repository.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController @RequestMapping("/transport") @RequiredArgsConstructor @Tag(name = "Transport")
public class TransportController {
    private final TransportRouteRepository routeRepo;
    private final TransportAssignmentRepository assignRepo;
    private final VehicleRepository vehicleRepo;
    private final PupilRepository pupilRepo;

    @GetMapping("/vehicles") public List<Vehicle> vehicles() { return vehicleRepo.findAll(); }
    @PostMapping("/vehicles") @ResponseStatus(HttpStatus.CREATED) public Vehicle createVehicle(@RequestBody VehicleReq req) {
        return vehicleRepo.save(Vehicle.builder().regNo(req.getRegNo()).model(req.getModel()).capacity(req.getCapacity())
                .driverName(req.getDriverName()).driverPhone(req.getDriverPhone()).notes(req.getNotes()).build()); }
    @DeleteMapping("/vehicles/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteVehicle(@PathVariable UUID id) { vehicleRepo.deleteById(id); }

    @GetMapping("/routes") public List<TransportRoute> routes() { return routeRepo.findAll(); }
    @PostMapping("/routes") @ResponseStatus(HttpStatus.CREATED) public TransportRoute createRoute(@RequestBody RouteReq req) {
        var route = TransportRoute.builder().name(req.getName()).pickupPoints(req.getPickupPoints()).fee(req.getFee()).build();
        if (req.getVehicleId() != null) vehicleRepo.findById(req.getVehicleId()).ifPresent(route::setVehicle);
        return routeRepo.save(route);
    }
    @DeleteMapping("/routes/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteRoute(@PathVariable UUID id) { routeRepo.deleteById(id); }

    @GetMapping("/assignments") public List<TransportAssignment> assignments(@RequestParam(required = false) UUID routeId) {
        if (routeId != null) return assignRepo.findByRouteId(routeId);
        return assignRepo.findAll();
    }
    @PostMapping("/assignments") @ResponseStatus(HttpStatus.CREATED) public TransportAssignment assign(@RequestBody AssignReq req) {
        var pupil = pupilRepo.findById(req.getPupilId()).orElseThrow(() -> new EntityNotFoundException("Pupil not found"));
        var route = routeRepo.findById(req.getRouteId()).orElseThrow(() -> new EntityNotFoundException("Route not found"));
        return assignRepo.save(TransportAssignment.builder().pupil(pupil).route(route).pickupPoint(req.getPickupPoint()).build());
    }
    @DeleteMapping("/assignments/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deleteAssign(@PathVariable UUID id) { assignRepo.deleteById(id); }

    @Data public static class VehicleReq { private String regNo; private String model; private Integer capacity; private String driverName; private String driverPhone; private String notes; }
    @Data public static class RouteReq { private String name; private String pickupPoints; private BigDecimal fee; private UUID vehicleId; }
    @Data public static class AssignReq { private UUID pupilId; private UUID routeId; private String pickupPoint; }
}
