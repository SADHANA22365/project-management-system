package com.company.pms.dto.response;

import com.company.pms.enums.TaskPriority;
import com.company.pms.enums.TaskStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
        List<LabelResponse> labels,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}