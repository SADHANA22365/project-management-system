package com.company.pms.dto.request;

import com.company.pms.enums.ProjectRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(

        @NotNull(message = "Project role is required")
        ProjectRole projectRole
) {
}