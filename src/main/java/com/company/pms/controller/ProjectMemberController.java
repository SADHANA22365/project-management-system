package com.company.pms.controller;

import com.company.pms.dto.request.AddMemberRequest;
import com.company.pms.dto.request.UpdateMemberRoleRequest;
import com.company.pms.dto.response.MemberResponse;
import com.company.pms.service.ProjectMemberService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMemberService memberService;

    public ProjectMemberController(ProjectMemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping
    public ResponseEntity<MemberResponse> addMember(@PathVariable Long projectId,
                                                    @Valid @RequestBody AddMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(memberService.addMember(projectId, request));
    }

    @GetMapping
    public List<MemberResponse> getMembers(@PathVariable Long projectId) {
        return memberService.getMembers(projectId);
    }

    @PatchMapping("/{userId}")
    public MemberResponse updateRole(@PathVariable Long projectId,
                                     @PathVariable Long userId,
                                     @Valid @RequestBody UpdateMemberRoleRequest request) {
        return memberService.updateRole(projectId, userId, request);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long projectId,
                                             @PathVariable Long userId) {
        memberService.removeMember(projectId, userId);
        return ResponseEntity.noContent().build();
    }
}