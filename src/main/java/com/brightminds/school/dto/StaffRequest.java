package com.brightminds.school.dto;

import com.brightminds.school.entity.enums.StaffStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class StaffRequest {
    @NotBlank
    private String fullName;
    private String staffNo;
    private String gender;
    private String email;
    private String phone;
    private String address;
    private LocalDate dob;
    private LocalDate dateJoined;
    private String employmentType;
    private boolean isTeacher;
    private BigDecimal basicSalary;
    private String qualifications;
    private String nextOfKin;
    private String bankName;
    private String bankAccount;
    private String photoUrl;
    private StaffStatus status;

    // Explicit accessors: Jackson strips the "is" prefix from Lombok's isTeacher()/setTeacher()
    // by default, which would bind this as "teacher" instead of "isTeacher".
    @JsonProperty("isTeacher")
    public boolean isTeacher() { return isTeacher; }
    @JsonProperty("isTeacher")
    public void setTeacher(boolean isTeacher) { this.isTeacher = isTeacher; }
}
