package com.company.pms.mapper;

import com.company.pms.dto.response.TaskResponse;
import com.company.pms.entity.Task;
import com.company.pms.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class TaskMapper {

    public TaskResponse toResponse(Task task, LocalDate today) {
        User assignee = task.getAssignee();
        User reporter = task.getReporter();

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getPriority(),
                task.getStatus(),
                task.getDueDate(),
                task.getEstimatedHours(),
                task.isOverdue(today),
                task.getProject().getId(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : nameOf(assignee),
                reporter.getId(),
                nameOf(reporter),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private String nameOf(User user) {
        return user.getFullName(); // ASSUMPTION: match what ProjectMapper uses for managerName
    }
}