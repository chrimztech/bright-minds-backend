package com.brightminds.school.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {
    // Either an email address or a phone number — parents may log in with either.
    @NotBlank
    private String identifier;
    @NotBlank
    private String password;
}
