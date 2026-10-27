package com.company.pms.dto.response;

import com.company.pms.enums.ProjectStatus;
import com.company.pms.enums.TaskStatus;

import java.util.Map;

public record DashboardResponse(
        long totalProjects,
        Map<ProjectStatus, Long> projectsByStatus,
        long totalTasks,
        Map<TaskStatus, Long> tasksByStatus,
        long overdueTasks,
        long totalUsers,
        long activeUsers
) {
}
