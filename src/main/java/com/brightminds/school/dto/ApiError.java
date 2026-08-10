package com.brightminds.school.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data @Builder
public class ApiError {
    private int status;
    private String message;
    private Instant timestamp;

    public static ApiError of(int status, String message) {
        return ApiError.builder()
                .status(status)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
}
