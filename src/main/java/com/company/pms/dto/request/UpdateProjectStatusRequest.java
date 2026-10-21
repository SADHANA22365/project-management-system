package com.company.pms.dto.request;

import com.company.pms.enums.ProjectStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateProjectStatusRequest(

        @NotNull(message = "Status is required")
        ProjectStatus status
) {
}