package com.company.pms.dto.request;

import com.company.pms.enums.IssueSeverity;
import com.company.pms.enums.IssueType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIssueRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @NotNull(message = "Type is required")
        IssueType type,

        IssueSeverity severity,

        Long assigneeId,

        @NotNull(message = "Reporter id is required")
        Long reporterId
) {
}