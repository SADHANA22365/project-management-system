package com.company.pms.mapper;

import com.company.pms.dto.response.MemberResponse;
import com.company.pms.entity.ProjectMember;
import com.company.pms.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ProjectMemberMapper {

    public MemberResponse toResponse(ProjectMember member) {
        User user = member.getUser();
        return new MemberResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getDesignation(),
                user.isActive(),
                member.getProjectRole(),
                member.getJoinedOn());
    }
}