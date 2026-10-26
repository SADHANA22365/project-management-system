package com.company.pms.dto.request;

import com.company.pms.enums.TaskPriority;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTaskRequest(

        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        TaskPriority priority,

        LocalDate dueDate,

        @DecimalMin(value = "0.5", message = "Estimated hours must be at least 0.5")
        @DecimalMax(value = "500", message = "Estimated hours must be at most 500")
        @Digits(integer = 3, fraction = 1, message = "Estimated hours allows at most 1 decimal place")
        BigDecimal estimatedHours,

        Long assigneeId,

        @NotNull(message = "Reporter id is required")
        Long reporterId
) {
}