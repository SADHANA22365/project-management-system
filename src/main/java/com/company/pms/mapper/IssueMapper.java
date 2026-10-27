package com.company.pms.mapper;

import com.company.pms.dto.response.IssueResponse;
import com.company.pms.entity.Issue;
import com.company.pms.entity.User;
import org.springframework.stereotype.Component;

@Component
public class IssueMapper {

    public IssueResponse toResponse(Issue issue) {
        User assignee = issue.getAssignee();
        User reporter = issue.getReporter();

        return new IssueResponse(
                issue.getId(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getType(),
                issue.getSeverity(),
                issue.getStatus(),
                issue.getResolvedAt(),
                issue.getProject().getId(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getFullName(),
                reporter.getId(),
                reporter.getFullName(),
                issue.getCreatedAt(),
                issue.getUpdatedAt());
    }
}