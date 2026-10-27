package com.company.pms.service.impl;

import com.company.pms.dto.request.CreateCommentRequest;
import com.company.pms.dto.request.UpdateCommentRequest;
import com.company.pms.dto.response.CommentResponse;
import com.company.pms.entity.Comment;
import com.company.pms.entity.Project;
import com.company.pms.entity.Task;
import com.company.pms.entity.User;
import com.company.pms.exception.BusinessRuleException;
import com.company.pms.exception.ResourceNotFoundException;
import com.company.pms.mapper.CommentMapper;
import com.company.pms.repository.CommentRepository;
import com.company.pms.repository.ProjectMemberRepository;
import com.company.pms.repository.TaskRepository;
import com.company.pms.repository.UserRepository;
import com.company.pms.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentServiceImpl.class);

    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final CommentMapper commentMapper;

    public CommentServiceImpl(CommentRepository commentRepository,
                              TaskRepository taskRepository,
                              UserRepository userRepository,
                              ProjectMemberRepository projectMemberRepository,
                              CommentMapper commentMapper) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.commentMapper = commentMapper;
    }

    @Override
    @Transactional
    public CommentResponse create(Long taskId, CreateCommentRequest request) {
        Task task = findTask(taskId);
        Project project = task.getProject();
        requireOpenProject(project);
        User author = requireActiveMember(project.getId(), request.authorId());

        Comment saved = commentRepository.save(
                new Comment(request.content().trim(), task, author));
        log.info("Created comment id={} on task id={}", saved.getId(), taskId);
        return commentMapper.toResponse(saved);
    }

    @Override
    public List<CommentResponse> getByTask(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new ResourceNotFoundException("Task", taskId);
        }
        return commentRepository.findByTaskIdOrderByCreatedAtAscIdAsc(taskId).stream()
                .map(commentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CommentResponse update(Long id, UpdateCommentRequest request) {
        Comment comment = findComment(id);
        requireOpenProject(comment.getTask().getProject());

        comment.setContent(request.content().trim());
        Comment saved = commentRepository.saveAndFlush(comment);
        log.info("Updated comment id={}", id);
        return commentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Comment comment = findComment(id);
        requireOpenProject(comment.getTask().getProject());
        commentRepository.delete(comment);
        log.info("Deleted comment id={}", id);
    }

    // ---------- helper methods ----------

    private Task findTask(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    private Comment findComment(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", id));
    }

    private void requireOpenProject(Project project) {
        if (project.getStatus().isClosed()) {
            throw new BusinessRuleException("Cannot change comments of a project that is "
                    + project.getStatus());
        }
    }

    private User requireActiveMember(Long projectId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Author", userId));
        if (!user.isActive()) {
            throw new BusinessRuleException("Author is not an active user");
        }
        if (!projectMemberRepository.existsByProjectIdAndUserId(projectId, userId)) {
            throw new BusinessRuleException("Author is not a member of this project");
        }
        return user;
    }
}