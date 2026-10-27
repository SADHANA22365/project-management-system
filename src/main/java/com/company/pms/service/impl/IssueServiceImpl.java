package com.company.pms.service.impl;

import com.company.pms.dto.request.CreateIssueRequest;
import com.company.pms.dto.request.UpdateIssueAssigneeRequest;
import com.company.pms.dto.request.UpdateIssueRequest;
import com.company.pms.dto.request.UpdateIssueStatusRequest;
import com.company.pms.dto.response.IssueResponse;
import com.company.pms.dto.response.PageResponse;
import com.company.pms.entity.Issue;
import com.company.pms.entity.Project;
import com.company.pms.entity.User;
import com.company.pms.enums.IssueSeverity;
import com.company.pms.enums.IssueStatus;
import com.company.pms.enums.IssueType;
import com.company.pms.exception.BusinessRuleException;
import com.company.pms.exception.InvalidRequestException;
import com.company.pms.exception.ResourceNotFoundException;
import com.company.pms.mapper.IssueMapper;
import com.company.pms.repository.IssueRepository;
import com.company.pms.repository.ProjectMemberRepository;
import com.company.pms.repository.ProjectRepository;
import com.company.pms.repository.UserRepository;
import com.company.pms.service.IssueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class IssueServiceImpl implements IssueService {

    private static final Logger log = LoggerFactory.getLogger(IssueServiceImpl.class);

    private static final Set<String> SORT_FIELDS = Set.of(
            "id", "title", "type", "severity", "status",
            "resolvedAt", "createdAt", "updatedAt");

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final IssueMapper issueMapper;

    public IssueServiceImpl(IssueRepository issueRepository,
                            ProjectRepository projectRepository,
                            UserRepository userRepository,
                            ProjectMemberRepository projectMemberRepository,
                            IssueMapper issueMapper) {
        this.issueRepository = issueRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.issueMapper = issueMapper;
    }

    @Override
    @Transactional
    public IssueResponse create(Long projectId, CreateIssueRequest request) {
        Project project = findProject(projectId);
        requireOpenProject(project);

        User reporter = requireActiveMember(projectId, request.reporterId(), "Reporter");
        User assignee = request.assigneeId() == null
                ? null
                : requireActiveMember(projectId, request.assigneeId(), "Assignee");

        Issue saved = issueRepository.save(new Issue(
                request.title().trim(),
                request.description(),
                request.type(),
                request.severity(),
                project,
                reporter,
                assignee));

        log.info("Created issue id={} in project id={}", saved.getId(), projectId);
        return issueMapper.toResponse(saved);
    }

    @Override
    public PageResponse<IssueResponse> getByProject(Long projectId, IssueStatus status,
                                                    IssueType type, IssueSeverity severity,
                                                    Pageable pageable) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project", projectId);
        }
        validateSortFields(pageable);

        Page<Issue> page = issueRepository.search(projectId, status, type, severity, pageable);
        return PageResponse.from(page.map(issueMapper::toResponse));
    }

    @Override
    public IssueResponse getById(Long id) {
        return issueMapper.toResponse(findIssue(id));
    }

    @Override
    @Transactional
    public IssueResponse update(Long id, UpdateIssueRequest request) {
        Issue issue = findIssue(id);
        requireOpenProject(issue.getProject());

        issue.setTitle(request.title().trim());
        issue.setDescription(request.description());
        issue.setType(request.type());
        issue.setSeverity(request.severity());

        Issue saved = issueRepository.saveAndFlush(issue);
        log.info("Updated issue id={}", id);
        return issueMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public IssueResponse updateStatus(Long id, UpdateIssueStatusRequest request) {
        Issue issue = findIssue(id);
        requireOpenProject(issue.getProject());

        IssueStatus current = issue.getStatus();
        IssueStatus target = request.status();
        if (!current.canTransitionTo(target)) {
            throw new BusinessRuleException(String.format(
                    "Cannot change issue status from %s to %s. Allowed: %s",
                    current, target, current.allowedTransitions()));
        }

        issue.changeStatus(target, LocalDateTime.now());
        Issue saved = issueRepository.saveAndFlush(issue);
        log.info("Issue id={} status {} -> {}", id, current, target);
        return issueMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public IssueResponse updateAssignee(Long id, UpdateIssueAssigneeRequest request) {
        Issue issue = findIssue(id);
        Project project = issue.getProject();
        requireOpenProject(project);

        if (request.assigneeId() == null) {
            issue.setAssignee(null);
            log.info("Unassigned issue id={}", id);
        } else {
            User assignee = requireActiveMember(project.getId(), request.assigneeId(), "Assignee");
            issue.setAssignee(assignee);
            log.info("Assigned issue id={} to user id={}", id, assignee.getId());
        }

        return issueMapper.toResponse(issueRepository.saveAndFlush(issue));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Issue issue = findIssue(id);
        requireOpenProject(issue.getProject());
        issueRepository.delete(issue);
        log.info("Deleted issue id={}", id);
    }

    // ---------- helper methods ----------

    private Issue findIssue(Long id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue", id));
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    }

    private void requireOpenProject(Project project) {
        if (project.getStatus().isClosed()) {
            throw new BusinessRuleException("Cannot change issues of a project that is "
                    + project.getStatus());
        }
    }

    private void validateSortFields(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!SORT_FIELDS.contains(order.getProperty())) {
                throw new InvalidRequestException("Invalid sort field '" + order.getProperty() + "'");
            }
        });
    }

    private User requireActiveMember(Long projectId, Long userId, String role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(role + " not found with id " + userId));
        if (!user.isActive()) {
            throw new BusinessRuleException(role + " is not an active user");
        }
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new BusinessRuleException(role + " is not a member of this project");
        }
        return user;
    }
}