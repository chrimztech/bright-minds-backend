package com.brightminds.school.dto;

import com.brightminds.school.entity.enums.PupilStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class PupilRequest {
    @NotBlank
    private String fullName;
    private String admissionNo;
    private String gender;
    private LocalDate dob;
    private UUID classId;
    private String address;
    private String town;
    private String province;
    private String postalCode;
    private String nationality;
    private String tribe;
    private String religion;
    private String homeLanguage;
    private String bloodGroup;
    private String allergies;
    private String medicalInfo;
    private String specialNeeds;
    private String nrcNo;
    private String birthCertNo;
    private String previousSchool;
    private String referralSource;
    private String siblingsInSchool;
    private String house;
    private String boardingStatus;
    private String transportMode;
    private String emergencyContact;
    private String photoUrl;
    private String notes;
    private LocalDate admittedOn;
    private PupilStatus status;
}
