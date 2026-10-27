package com.company.pms.dto.response;

import com.company.pms.enums.TaskStatus;

import java.util.Map;

public record ProjectProgressResponse(
        Long projectId,
        String projectName,
        long totalTasks,
        long doneTasks,
        double progressPercent,
        Map<TaskStatus, Long> tasksByStatus
) {
}