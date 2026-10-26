package com.company.pms.service.impl;

import com.company.pms.dto.request.CreateTaskRequest;
import com.company.pms.dto.request.UpdateTaskAssigneeRequest;
import com.company.pms.dto.request.UpdateTaskRequest;
import com.company.pms.dto.request.UpdateTaskStatusRequest;
import com.company.pms.dto.response.PageResponse;
import com.company.pms.dto.response.TaskResponse;
import com.company.pms.entity.Project;
import com.company.pms.entity.Task;
import com.company.pms.entity.User;
import com.company.pms.enums.TaskStatus;
import com.company.pms.exception.BusinessRuleException;
import com.company.pms.exception.ResourceNotFoundException;
import com.company.pms.mapper.TaskMapper;
import com.company.pms.repository.ProjectMemberRepository;
import com.company.pms.repository.ProjectRepository;
import com.company.pms.repository.TaskRepository;
import com.company.pms.repository.UserRepository;
import com.company.pms.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import com.company.pms.exception.InvalidRequestException;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);
    private static final Set<String> OVERDUE_SORT_FIELDS = Set.of(
            "id", "title", "priority", "status", "dueDate",
            "estimatedHours", "createdAt", "updatedAt");

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final TaskMapper taskMapper;

    public TaskServiceImpl(TaskRepository taskRepository,
                           ProjectRepository projectRepository,
                           UserRepository userRepository,
                           ProjectMemberRepository projectMemberRepository,
                           TaskMapper taskMapper) {
        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.taskMapper = taskMapper;
    }

    @Override
    @Transactional
    public TaskResponse create(Long projectId, CreateTaskRequest request) {
        Project project = findProject(projectId);
        requireOpenProject(project);
        validateDueDate(project, request.dueDate());

        User reporter = requireActiveMember(projectId, request.reporterId(), "Reporter");
        User assignee = request.assigneeId() == null
                ? null
                : requireActiveMember(projectId, request.assigneeId(), "Assignee");

        Task task = new Task(
                request.title(),
                request.description(),
                request.priority(),
                request.dueDate(),
                request.estimatedHours(),
                project,
                assignee,
                reporter);

        Task saved = taskRepository.save(task);
        log.info("Created task id={} in project id={}", saved.getId(), projectId);
        return taskMapper.toResponse(saved, LocalDate.now());
    }

    @Override
    public PageResponse<TaskResponse> getByProject(Long projectId, TaskStatus status, Pageable pageable) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("Project not found with id " + projectId);
        }
        Page<Task> page = (status == null)
                ? taskRepository.findByProjectId(projectId, pageable)
                : taskRepository.findByProjectIdAndStatus(projectId, status, pageable);

        LocalDate today = LocalDate.now();
        return PageResponse.from(page.map(task -> taskMapper.toResponse(task, today)));
    }

    @Override
    public TaskResponse getById(Long id) {
        Task task = findTask(id);
        return taskMapper.toResponse(task, LocalDate.now());
    }

    @Override
    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        Task task = findTask(id);
        Project project = task.getProject();
        requireOpenProject(project);
        validateDueDate(project, request.dueDate());

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setPriority(request.priority());
        task.setDueDate(request.dueDate());
        task.setEstimatedHours(request.estimatedHours());

        log.info("Updated task id={}", id);
        return taskMapper.toResponse(task, LocalDate.now());
    }

    @Override
    @Transactional
    public TaskResponse updateStatus(Long id, UpdateTaskStatusRequest request) {
        Task task = findTask(id);
        requireOpenProject(task.getProject());

        TaskStatus current = task.getStatus();
        TaskStatus target = request.status();
        if (!current.canTransitionTo(target)) {
            throw new BusinessRuleException("Cannot change task status from "
                    + current + " to " + target);
        }

        task.setStatus(target);
        log.info("Task id={} status {} -> {}", id, current, target);
        return taskMapper.toResponse(task, LocalDate.now());
    }

    @Override
    @Transactional
    public TaskResponse updateAssignee(Long id, UpdateTaskAssigneeRequest request) {
        Task task = findTask(id);
        Project project = task.getProject();
        requireOpenProject(project);

        if (request.assigneeId() == null) {
            task.setAssignee(null);
            log.info("Unassigned task id={}", id);
        } else {
            User assignee = requireActiveMember(project.getId(), request.assigneeId(), "Assignee");
            task.setAssignee(assignee);
            log.info("Assigned task id={} to user id={}", id, assignee.getId());
        }
        return taskMapper.toResponse(task, LocalDate.now());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Task task = findTask(id);
        requireOpenProject(task.getProject());
        taskRepository.delete(task);
        log.info("Deleted task id={}", id);
    }

        @Override
    public PageResponse<TaskResponse> getOverdue(Pageable pageable) {
        validateSortFields(pageable);
        LocalDate today = LocalDate.now();
        Page<Task> page = taskRepository.findOverdue(today, pageable);
        return PageResponse.from(page.map(task -> taskMapper.toResponse(task, today)));
    }

    private void validateSortFields(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!OVERDUE_SORT_FIELDS.contains(order.getProperty())) {
                throw new InvalidRequestException("Invalid sort field '" + order.getProperty() + "'");
            }
        });
    }

    // ---------- helper methods ----------

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
    }

    private Project findProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + projectId));
    }

    private void requireOpenProject(Project project) {
        if (project.getStatus().isClosed()) {
            throw new BusinessRuleException("Cannot change tasks of a project that is "
                    + project.getStatus());
        }
    }

    private void validateDueDate(Project project, LocalDate dueDate) {
        if (dueDate == null) {
            return;
        }
        if (dueDate.isBefore(project.getStartDate())) {
            throw new BusinessRuleException("Due date cannot be before the project start date");
        }
        if (project.getEndDate() != null && dueDate.isAfter(project.getEndDate())) {
            throw new BusinessRuleException("Due date cannot be after the project end date");
        }
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