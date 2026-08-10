package com.brightminds.school.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class PromotionDto {

    private PromotionDto() {}

    @Data
    public static class Request {
        private List<UUID> pupilIds;
        private UUID targetClassId;
        private UUID academicYearId;
        private LocalDate promotedOn;
        private String notes;
    }

    @Data
    public static class Result {
        private int promotedCount;
        private List<Record> promotions;
    }

    @Data
    public static class Record {
        private UUID id;
        private UUID pupilId;
        private String pupilName;
        private UUID fromClassId;
        private String fromClassName;
        private UUID toClassId;
        private String toClassName;
        private UUID academicYearId;
        private String academicYearName;
        private LocalDate promotedOn;
        private String notes;
    }
}
