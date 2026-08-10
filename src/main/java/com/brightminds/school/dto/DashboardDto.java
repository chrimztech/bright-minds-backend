package com.brightminds.school.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data @Builder
public class DashboardDto {
    private long totalPupils;
    private long activeStaff;
    private long totalClasses;
    private long presentToday;
    private BigDecimal collectedThisMonth;
    private List<AnnouncementSummary> recentAnnouncements;

    @Data @Builder
    public static class AnnouncementSummary {
        private String id;
        private String title;
        private String createdAt;
    }
}
