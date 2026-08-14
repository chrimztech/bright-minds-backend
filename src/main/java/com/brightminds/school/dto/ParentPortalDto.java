package com.brightminds.school.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ParentPortalDto {

    private ParentPortalDto() {}

    @Data @Builder
    public static class Dashboard {
        private GuardianInfo guardian;
        private List<ChildSummary> children;
    }

    @Data @Builder
    public static class GuardianInfo {
        private UUID id;
        private String fullName;
        private String relationship;
        private String email;
        private String phone;
    }

    @Data @Builder
    public static class ChildSummary {
        private UUID id;
        private String admissionNo;
        private String fullName;
        private String gender;
        private LocalDate dateOfBirth;
        private String status;
        private ClassInfo schoolClass;
        private AttendanceSummary attendance;
        private PerformanceSummary latestPerformance;
        private int reportCardCount;
    }

    @Data @Builder
    public static class ClassInfo {
        private UUID id;
        private String name;
        private String stream;
        private int levelOrder;
        private TeacherInfo classTeacher;
    }

    @Data @Builder
    public static class TeacherInfo {
        private UUID id;
        private String staffNo;
        private String fullName;
        private String email;
        private String phone;
        private String signatureUrl;
    }

    @Data @Builder
    public static class AttendanceSummary {
        private LocalDate from;
        private LocalDate to;
        private long total;
        private long present;
        private long absent;
        private long late;
        private long sick;
        private long excused;
        private double percentage;
    }

    @Data @Builder
    public static class PerformanceSummary {
        private UUID examId;
        private String examName;
        private String termName;
        private String academicYearName;
        private double averagePercentage;
        private String grade;
    }

    @Data @Builder
    public static class ReportCard {
        private UUID pupilId;
        private String pupilName;
        private String admissionNo;
        private UUID examId;
        private String examName;
        private String assessmentType;
        private LocalDate examDate;
        private UUID termId;
        private String termName;
        private UUID academicYearId;
        private String academicYearName;
        private ClassInfo schoolClass;
        private List<SubjectResult> subjects;
        private double totalScore;
        private double totalOutOf;
        private double averagePercentage;
        private String overallGrade;
        private AttendanceSummary attendance;
        private String classTeacherRemark;
        private String headTeacherRemark;
    }

    @Data @Builder
    public static class SubjectResult {
        private UUID subjectId;
        private String subjectName;
        private double score;
        private double outOf;
        private double percentage;
        private String grade;
        private String comment;
    }
}
