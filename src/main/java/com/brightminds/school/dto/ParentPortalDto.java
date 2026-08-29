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
        private UUID termId;
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
        private UUID termId;
        private String termName;
        private UUID academicYearId;
        private String academicYearName;
        private ClassInfo schoolClass;
        private List<SubjectPivotResult> subjects;
        private double termAveragePercentage;
        private String overallGrade;
        private AttendanceSummary attendance;
        private String classTeacherRemark;
        private String headTeacherRemark;
        // Rank of this pupil's term average among their classmates who also have marks this
        // term — null when the pupil has no resolvable class (nothing to rank against).
        private Integer position;
        private Integer classSize;
    }

    @Data @Builder
    public static class AssessmentCell {
        private double score;
        private double outOf;
        private double percentage;
    }

    @Data @Builder
    public static class SubjectPivotResult {
        private UUID subjectId;
        private String subjectName;
        private AssessmentCell test1;
        private AssessmentCell test2;
        private AssessmentCell midTerm;
        private AssessmentCell endOfTerm;
        // Mean of whichever of the 4 cells above are present — null only if a subject row
        // somehow exists with zero populated cells, which shouldn't happen in practice.
        private Double averagePercentage;
        private String grade;
        private String comment;
    }
}
