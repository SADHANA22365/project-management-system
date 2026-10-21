package com.company.pms.dto.response;

import com.company.pms.enums.Designation;
import com.company.pms.enums.ProjectRole;

import java.time.LocalDate;

public record MemberResponse(
        Long userId,
        String fullName,
        String email,
        Designation designation,
        boolean active,
        ProjectRole projectRole,
        LocalDate joinedOn
) {
}