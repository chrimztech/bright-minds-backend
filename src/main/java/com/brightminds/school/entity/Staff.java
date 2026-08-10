package com.brightminds.school.entity;

import com.brightminds.school.entity.enums.StaffStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "staff")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Staff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "staff_no", nullable = false, unique = true)
    private String staffNo;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String gender;
    private String email;
    private String phone;
    private String address;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "date_joined")
    private LocalDate dateJoined;

    @Column(name = "employment_type")
    private String employmentType;

    @Column(name = "is_teacher")
    @Builder.Default
    private boolean isTeacher = false;

    // Explicit accessors: Jackson strips the "is" prefix from Lombok's isTeacher()/setTeacher()
    // by default, which would serialize/bind this as "teacher" instead of "isTeacher".
    @JsonProperty("isTeacher")
    public boolean isTeacher() { return isTeacher; }

    @JsonProperty("isTeacher")
    public void setTeacher(boolean isTeacher) { this.isTeacher = isTeacher; }

    @Column(name = "basic_salary", precision = 12, scale = 2)
    private BigDecimal basicSalary;

    private String qualifications;

    @Column(name = "next_of_kin")
    private String nextOfKin;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_account")
    private String bankAccount;

    @Column(name = "photo_url")
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private StaffStatus status = StaffStatus.ACTIVE;

    @Column(name = "user_id")
    private UUID userId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
