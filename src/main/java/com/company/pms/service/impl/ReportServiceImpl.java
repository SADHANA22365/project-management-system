package com.company.pms.service.impl;

import com.company.pms.dto.response.ProjectProgressResponse;
import com.company.pms.entity.Project;
import com.company.pms.enums.TaskStatus;
import com.company.pms.exception.ResourceNotFoundException;
import com.company.pms.repository.ProjectRepository;
import com.company.pms.repository.TaskRepository;
import com.company.pms.repository.TaskStatusCount;
import com.company.pms.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public ReportServiceImpl(ProjectRepository projectRepository,
                             TaskRepository taskRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public ProjectProgressResponse getProjectProgress(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        Map<TaskStatus, Long> tasksByStatus = new EnumMap<>(TaskStatus.class);
        for (TaskStatus status : TaskStatus.values()) {
            tasksByStatus.put(status, 0L);
        }
        for (TaskStatusCount row : taskRepository.countByStatusForProject(projectId)) {
            tasksByStatus.put(row.getStatus(), row.getTotal());
        }

        long total = tasksByStatus.values().stream().mapToLong(Long::longValue).sum();
        long done = tasksByStatus.get(TaskStatus.DONE);
        double percent = total == 0 ? 0.0 : Math.round(done * 1000.0 / total) / 10.0;

        return new ProjectProgressResponse(
                project.getId(),
                project.getName(),
                total,
                done,
                percent,
                tasksByStatus);
    }
}