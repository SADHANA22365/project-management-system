package com.company.pms.dto.response;

import com.company.pms.enums.ProjectStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        ProjectStatus status,
        Long managerId,
        String managerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}