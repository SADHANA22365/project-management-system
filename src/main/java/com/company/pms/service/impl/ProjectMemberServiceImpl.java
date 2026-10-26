package com.company.pms.service.impl;

import com.company.pms.dto.request.AddMemberRequest;
import com.company.pms.dto.request.UpdateMemberRoleRequest;
import com.company.pms.dto.response.MemberResponse;
import com.company.pms.entity.Project;
import com.company.pms.entity.ProjectMember;
import com.company.pms.entity.User;
import com.company.pms.enums.ProjectRole;
import com.company.pms.enums.TaskStatus;
import com.company.pms.exception.BusinessRuleException;
import com.company.pms.exception.DuplicateResourceException;
import com.company.pms.exception.ResourceNotFoundException;
import com.company.pms.mapper.ProjectMemberMapper;
import com.company.pms.repository.ProjectMemberRepository;
import com.company.pms.repository.ProjectRepository;
import com.company.pms.repository.TaskRepository;
import com.company.pms.repository.UserRepository;
import com.company.pms.service.ProjectMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private static final Logger log = LoggerFactory.getLogger(ProjectMemberServiceImpl.class);

    private final ProjectMemberRepository memberRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final ProjectMemberMapper memberMapper;

    public ProjectMemberServiceImpl(ProjectMemberRepository memberRepository,
                                    ProjectRepository projectRepository,
                                    UserRepository userRepository,
                                    TaskRepository taskRepository,
                                    ProjectMemberMapper memberMapper) {
        this.memberRepository = memberRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.taskRepository = taskRepository;
        this.memberMapper = memberMapper;
    }

    @Override
    @Transactional
    public MemberResponse addMember(Long projectId, AddMemberRequest request) {
        Project project = findProjectOrThrow(projectId);
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.userId()));

        if (!user.isActive()) {
            throw new BusinessRuleException("Inactive users cannot be added to a project");
        }
        if (memberRepository.existsByProjectIdAndUserId(projectId, user.getId())) {
            throw new DuplicateResourceException("User " + user.getId()
                    + " is already a member of project " + projectId);
        }

        ProjectMember saved = memberRepository.save(
                new ProjectMember(project, user, request.projectRole()));
        log.info("Added user id={} to project id={} as {}",
                user.getId(), projectId, request.projectRole());
        return memberMapper.toResponse(saved);
    }

    @Override
    public List<MemberResponse> getMembers(Long projectId) {
        findProjectOrThrow(projectId);
        return memberRepository.findByProjectIdOrderByJoinedOnAscIdAsc(projectId).stream()
                .map(memberMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public MemberResponse updateRole(Long projectId, Long userId, UpdateMemberRoleRequest request) {
        ProjectMember member = findMemberOrThrow(projectId, userId);
        ProjectRole newRole = request.projectRole();

        if (member.getProjectRole() == ProjectRole.LEAD && newRole != ProjectRole.LEAD) {
            ensureAnotherLeadExists(projectId);
        }

        member.setProjectRole(newRole);
        log.info("Project id={} user id={} role -> {}", projectId, userId, newRole);
        return memberMapper.toResponse(member);
    }

    @Override
    @Transactional
    public void removeMember(Long projectId, Long userId) {
        ProjectMember member = findMemberOrThrow(projectId, userId);

        if (member.getProjectRole() == ProjectRole.LEAD) {
            ensureAnotherLeadExists(projectId);
        }
        if (taskRepository.existsByAssigneeIdAndProjectIdAndStatusNot(
                userId, projectId, TaskStatus.DONE)) {
            throw new BusinessRuleException(
                    "Cannot remove a member who still has open tasks in this project");
        }

        memberRepository.delete(member);
        log.info("Removed user id={} from project id={}", userId, projectId);
    }

    private void ensureAnotherLeadExists(Long projectId) {
        long leads = memberRepository.countByProjectIdAndProjectRole(projectId, ProjectRole.LEAD);
        if (leads <= 1) {
            throw new BusinessRuleException("A project must always have at least one LEAD");
        }
    }

    private Project findProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    }

    private ProjectMember findMemberOrThrow(Long projectId, Long userId) {
        return memberRepository.findByProjectIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User " + userId + " is not a member of project " + projectId));
    }
}