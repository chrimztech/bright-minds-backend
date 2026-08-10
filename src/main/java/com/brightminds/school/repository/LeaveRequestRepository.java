package com.brightminds.school.repository;

import com.brightminds.school.entity.LeaveRequest;
import com.brightminds.school.entity.enums.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    List<LeaveRequest> findByStaffId(UUID staffId);
    List<LeaveRequest> findByStatus(LeaveStatus status);
    List<LeaveRequest> findAllByOrderByCreatedAtDesc();
}
