package com.company.pms.dto.request;

import com.company.pms.enums.IssueStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateIssueStatusRequest(

        @NotNull(message = "Status is required")
        IssueStatus status
) {
}