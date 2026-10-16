package com.company.pms.dto.response;

import com.company.pms.enums.Designation;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        Designation designation,
        boolean active,
        String phoneNumber,
        String bio,
        String location,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}