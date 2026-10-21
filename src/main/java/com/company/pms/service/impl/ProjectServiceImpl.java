package com.company.pms.service.impl;

import com.company.pms.dto.request.CreateProjectRequest;
import com.company.pms.dto.request.UpdateProjectRequest;
import com.company.pms.dto.request.UpdateProjectStatusRequest;
import com.company.pms.dto.response.PageResponse;
import com.company.pms.dto.response.ProjectResponse;
import com.company.pms.entity.Project;
import com.company.pms.entity.User;
import com.company.pms.enums.ProjectStatus;
import com.company.pms.exception.BusinessRuleException;
import com.company.pms.exception.DuplicateResourceException;
import com.company.pms.exception.ResourceNotFoundException;
import com.company.pms.mapper.ProjectMapper;
import com.company.pms.repository.ProjectRepository;
import com.company.pms.repository.UserRepository;
import com.company.pms.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.company.pms.entity.ProjectMember;
import com.company.pms.enums.ProjectRole;
import com.company.pms.repository.ProjectMemberRepository;

import java.time.LocalDate;

@Service
@Transactional(readOnly = true)
public class ProjectServiceImpl implements ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectServiceImpl.class);

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMapper projectMapper;
    private final ProjectMemberRepository memberRepository;

   public ProjectServiceImpl(ProjectRepository projectRepository,
                          UserRepository userRepository,
                          ProjectMemberRepository memberRepository,
                          ProjectMapper projectMapper) {
    this.projectRepository = projectRepository;
    this.userRepository = userRepository;
    this.memberRepository = memberRepository;
    this.projectMapper = projectMapper;
}

    @Override
    @Transactional
    public ProjectResponse create(CreateProjectRequest request) {
        String name = request.name().trim();
        if (projectRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A project named '" + name + "' already exists");
        }
        validateDates(request.startDate(), request.endDate());

        User manager = userRepository.findById(request.managerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.managerId()));
        if (!manager.isActive()) {
            throw new BusinessRuleException("The manager must be an active user");
        }

        Project saved = projectRepository.save(projectMapper.toEntity(request, manager));
        memberRepository.save(new ProjectMember(saved, manager, ProjectRole.LEAD));
        log.info("Created project id={}", saved.getId());
        return projectMapper.toResponse(saved);
    }

    @Override
    public ProjectResponse getById(Long id) {
        return projectMapper.toResponse(findProjectOrThrow(id));
    }

    @Override
    public PageResponse<ProjectResponse> getAll(ProjectStatus status, Pageable pageable) {
        Page<Project> projects = (status == null)
                ? projectRepository.findAll(pageable)
                : projectRepository.findByStatus(status, pageable);
        return PageResponse.from(projects.map(projectMapper::toResponse));
    }

    @Override
    @Transactional
    public ProjectResponse update(Long id, UpdateProjectRequest request) {
        Project project = findProjectOrThrow(id);
        String name = request.name().trim();
        if (projectRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("A project named '" + name + "' already exists");
        }
        validateDates(request.startDate(), request.endDate());

        projectMapper.updateEntity(project, request);
        log.info("Updated project id={}", id);
        return projectMapper.toResponse(project);
    }

    @Override
    @Transactional
    public ProjectResponse updateStatus(Long id, UpdateProjectStatusRequest request) {
        Project project = findProjectOrThrow(id);
        ProjectStatus current = project.getStatus();
        ProjectStatus target = request.status();

        if (!current.canTransitionTo(target)) {
            throw new BusinessRuleException(String.format(
                    "Cannot change project status from %s to %s. Allowed: %s",
                    current, target, current.allowedTransitions()));
        }
        // TODO (Phase 5): if target is COMPLETED, require every task to be DONE

        project.setStatus(target);
        log.info("Project id={} status {} -> {}", id, current, target);
        return projectMapper.toResponse(project);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Project project = findProjectOrThrow(id);
        projectRepository.delete(project);
        log.info("Deleted project id={}", id);
    }

    private Project findProjectOrThrow(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new BusinessRuleException("End date cannot be before start date");
        }
    }
}