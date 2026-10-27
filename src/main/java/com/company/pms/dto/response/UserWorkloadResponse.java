package com.company.pms.dto.response;

import com.company.pms.enums.TaskStatus;

import java.util.Map;

public record UserWorkloadResponse(
        Long userId,
        String userName,
        long openTasks,
        long overdueTasks,
        Map<TaskStatus, Long> assignedByStatus
) {
}