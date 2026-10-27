package com.company.pms.service.impl;

import com.company.pms.dto.response.DashboardResponse;
import com.company.pms.dto.response.ProjectProgressResponse;
import com.company.pms.entity.Project;
import com.company.pms.enums.ProjectStatus;
import com.company.pms.enums.TaskStatus;
import com.company.pms.exception.ResourceNotFoundException;
import com.company.pms.repository.ProjectRepository;
import com.company.pms.repository.ProjectStatusCount;
import com.company.pms.repository.TaskRepository;
import com.company.pms.repository.TaskStatusCount;
import com.company.pms.repository.UserRepository;
import com.company.pms.service.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public ReportServiceImpl(ProjectRepository projectRepository,
                             TaskRepository taskRepository,
                             UserRepository userRepository) {
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    @Override
    public ProjectProgressResponse getProjectProgress(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        Map<TaskStatus, Long> tasksByStatus = emptyCounts(TaskStatus.class);
        for (TaskStatusCount row : taskRepository.countByStatusForProject(projectId)) {
            tasksByStatus.put(row.getStatus(), row.getTotal());
        }

        long total = sum(tasksByStatus);
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

    @Override
    public DashboardResponse getDashboard() {
        Map<ProjectStatus, Long> projectsByStatus = emptyCounts(ProjectStatus.class);
        for (ProjectStatusCount row : projectRepository.countGroupedByStatus()) {
            projectsByStatus.put(row.getStatus(), row.getTotal());
        }

        Map<TaskStatus, Long> tasksByStatus = emptyCounts(TaskStatus.class);
        for (TaskStatusCount row : taskRepository.countGroupedByStatus()) {
            tasksByStatus.put(row.getStatus(), row.getTotal());
        }

        return new DashboardResponse(
                sum(projectsByStatus),
                projectsByStatus,
                sum(tasksByStatus),
                tasksByStatus,
                taskRepository.countOverdue(LocalDate.now()),
                userRepository.count(),
                userRepository.countByActiveTrue());
    }

    // ---------- helper methods ----------

    private <E extends Enum<E>> Map<E, Long> emptyCounts(Class<E> type) {
        Map<E, Long> counts = new EnumMap<>(type);
        for (E value : type.getEnumConstants()) {
            counts.put(value, 0L);
        }
        return counts;
    }

    private long sum(Map<?, Long> counts) {
        return counts.values().stream().mapToLong(Long::longValue).sum();
    }
}