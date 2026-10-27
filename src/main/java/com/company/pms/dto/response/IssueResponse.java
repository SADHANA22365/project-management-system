package com.company.pms.dto.response;

import com.company.pms.enums.IssueSeverity;
import com.company.pms.enums.IssueStatus;
import com.company.pms.enums.IssueType;

import java.time.LocalDateTime;

public record IssueResponse(
        Long id,
        String title,
        String description,
        IssueType type,
        IssueSeverity severity,
        IssueStatus status,
        LocalDateTime resolvedAt,
        Long projectId,
        Long assigneeId,
        String assigneeName,
        Long reporterId,
        String reporterName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}