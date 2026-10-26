package com.company.pms.dto.request;

import com.company.pms.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(

        @NotNull(message = "Status is required")
        TaskStatus status
) {
}