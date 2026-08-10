package com.brightminds.school.controller;

import com.brightminds.school.entity.LeaveRequest;
import com.brightminds.school.entity.enums.LeaveStatus;
import com.brightminds.school.repository.LeaveRequestRepository;
import com.brightminds.school.repository.StaffRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/leave")
@RequiredArgsConstructor
@Tag(name = "Leave Management")
@PreAuthorize("hasAnyRole('SUPER_ADMIN','ADMIN','HEAD_TEACHER','DEPUTY_HEAD')")
public class LeaveController {

    private final LeaveRequestRepository leaveRepo;
    private final StaffRepository staffRepo;

    @GetMapping
    public List<LeaveRequest> list(@RequestParam(required = false) UUID staffId,
                                   @RequestParam(required = false) String status) {
        if (staffId != null) return leaveRepo.findByStaffId(staffId);
        if (status != null) return leaveRepo.findByStatus(LeaveStatus.valueOf(status.toUpperCase()));
        return leaveRepo.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeaveRequest create(@RequestBody LeaveRequestBody req) {
        var staff = staffRepo.findById(req.getStaffId())
                .orElseThrow(() -> new EntityNotFoundException("Staff not found"));
        return leaveRepo.save(LeaveRequest.builder()
                .staff(staff)
                .leaveType(req.getLeaveType())
                .startDate(req.getStartDate())
                .endDate(req.getEndDate())
                .reason(req.getReason())
                .build());
    }

    @PatchMapping("/{id}/approve")
    public LeaveRequest approve(@PathVariable UUID id) {
        LeaveRequest req = leaveRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Leave request not found"));
        req.setStatus(LeaveStatus.APPROVED);
        return leaveRepo.save(req);
    }

    @PatchMapping("/{id}/reject")
    public LeaveRequest reject(@PathVariable UUID id) {
        LeaveRequest req = leaveRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Leave request not found"));
        req.setStatus(LeaveStatus.REJECTED);
        return leaveRepo.save(req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) { leaveRepo.deleteById(id); }

    @Data
    public static class LeaveRequestBody {
        private UUID staffId;
        private String leaveType;
        private LocalDate startDate;
        private LocalDate endDate;
        private String reason;
    }
}
