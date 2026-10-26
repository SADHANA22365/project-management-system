package com.company.pms.dto.response;

import com.company.pms.enums.TaskPriority;
import com.company.pms.enums.TaskStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskPriority priority,
        TaskStatus status,
        LocalDate dueDate,
        BigDecimal estimatedHours,
        boolean overdue,
        Long projectId,
        Long assigneeId,
        String assigneeName,
        Long reporterId,
        String reporterName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}