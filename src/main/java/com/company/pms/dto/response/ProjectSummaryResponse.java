package com.company.pms.dto.response;

import com.company.pms.enums.ProjectStatus;

import java.time.LocalDate;

public record ProjectSummaryResponse(
        Long projectId,
        String name,
        ProjectStatus status,
        LocalDate startDate,
        LocalDate endDate,
        String managerName,
        long memberCount,
        long overdueTasks,
        ProjectProgressResponse progress
) {
}