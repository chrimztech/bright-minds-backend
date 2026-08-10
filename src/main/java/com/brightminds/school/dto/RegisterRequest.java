package com.brightminds.school.dto;

import com.brightminds.school.entity.enums.AppRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class RegisterRequest {
    @NotBlank @Email
    private String email;

    @NotBlank @Size(min = 6)
    private String password;

    @NotBlank
    private String fullName;

    private String phone;

    private List<AppRole> roles;
}
