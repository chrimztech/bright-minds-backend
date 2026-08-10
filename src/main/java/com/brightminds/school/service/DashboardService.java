package com.brightminds.school.service;

import com.brightminds.school.dto.DashboardDto;
import com.brightminds.school.entity.enums.AttendanceStatus;
import com.brightminds.school.entity.enums.PupilStatus;
import com.brightminds.school.entity.enums.StaffStatus;
import com.brightminds.school.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final PupilRepository pupilRepo;
    private final StaffRepository staffRepo;
    private final SchoolClassRepository classRepo;
    private final AttendanceRepository attendanceRepo;
    private final PaymentRepository paymentRepo;
    private final AnnouncementRepository announcementRepo;

    public DashboardDto getDashboard() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.with(TemporalAdjusters.firstDayOfMonth());

        long pupils = pupilRepo.countByStatus(PupilStatus.ACTIVE);
        long staff = staffRepo.countByStatus(StaffStatus.ACTIVE);
        long classes = classRepo.count();
        long present = attendanceRepo.countByDateAndStatus(today, AttendanceStatus.PRESENT);
        BigDecimal collected = paymentRepo.sumAmountFrom(monthStart);
        if (collected == null) collected = BigDecimal.ZERO;

        var recentAnn = announcementRepo.findTop5ByOrderByCreatedAtDesc().stream()
                .map(a -> DashboardDto.AnnouncementSummary.builder()
                        .id(a.getId().toString())
                        .title(a.getTitle())
                        .createdAt(a.getCreatedAt().toString())
                        .build())
                .collect(Collectors.toList());

        return DashboardDto.builder()
                .totalPupils(pupils)
                .activeStaff(staff)
                .totalClasses(classes)
                .presentToday(present)
                .collectedThisMonth(collected)
                .recentAnnouncements(recentAnn)
                .build();
    }
}
