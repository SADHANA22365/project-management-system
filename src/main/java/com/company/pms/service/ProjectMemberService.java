package com.company.pms.service;

import com.company.pms.dto.request.AddMemberRequest;
import com.company.pms.dto.request.UpdateMemberRoleRequest;
import com.company.pms.dto.response.MemberResponse;

import java.util.List;

public interface ProjectMemberService {

    MemberResponse addMember(Long projectId, AddMemberRequest request);

    List<MemberResponse> getMembers(Long projectId);

    MemberResponse updateRole(Long projectId, Long userId, UpdateMemberRoleRequest request);

    void removeMember(Long projectId, Long userId);
}