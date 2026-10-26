package com.company.pms.mapper;

import com.company.pms.dto.response.LabelResponse;
import com.company.pms.dto.response.TaskResponse;
import com.company.pms.entity.Label;
import com.company.pms.entity.Task;
import com.company.pms.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Component
public class TaskMapper {

    private final LabelMapper labelMapper;

    public TaskMapper(LabelMapper labelMapper) {
        this.labelMapper = labelMapper;
    }

    public TaskResponse toResponse(Task task, LocalDate today) {
        User assignee = task.getAssignee();
        User reporter = task.getReporter();

        List<LabelResponse> labels = task.getLabels().stream()
                .sorted(Comparator.comparing(Label::getName, String.CASE_INSENSITIVE_ORDER))
                .map(labelMapper::toResponse)
                .toList();

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
                labels,
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private String nameOf(User user) {
        return user.getFullName();
    }
}