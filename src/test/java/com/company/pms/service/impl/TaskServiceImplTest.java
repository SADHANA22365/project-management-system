package com.company.pms.service.impl;

import com.company.pms.dto.request.CreateTaskRequest;
import com.company.pms.dto.request.UpdateTaskAssigneeRequest;
import com.company.pms.dto.request.UpdateTaskStatusRequest;
import com.company.pms.dto.response.TaskResponse;
import com.company.pms.entity.Project;
import com.company.pms.entity.Task;
import com.company.pms.entity.User;
import com.company.pms.enums.ProjectStatus;
import com.company.pms.enums.TaskPriority;
import com.company.pms.enums.TaskStatus;
import com.company.pms.exception.BusinessRuleException;
import com.company.pms.exception.InvalidRequestException;
import com.company.pms.mapper.TaskMapper;
import com.company.pms.repository.CommentRepository;
import com.company.pms.repository.LabelRepository;
import com.company.pms.repository.ProjectMemberRepository;
import com.company.pms.repository.ProjectRepository;
import com.company.pms.repository.TaskRepository;
import com.company.pms.repository.UserRepository;
import com.company.pms.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceImplTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private LabelRepository labelRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private TaskMapper taskMapper;

    @InjectMocks
    private TaskServiceImpl taskService;

    private User reporter;
    private Project project;

    @BeforeEach
    void setUp() {
        reporter = TestData.user(1L, "Ann Lee");
        project = TestData.project(10L, ProjectStatus.ACTIVE, reporter);
    }

    private static TaskResponse responseFor(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), null, task.getPriority(),
                task.getStatus(), null, null, false, 10L, null, null, 1L, "Ann Lee",
                List.of(), null, null);
    }

    private static CreateTaskRequest createRequest(LocalDate dueDate, Long assigneeId) {
        return new CreateTaskRequest("Fix bug", "details", null, dueDate, null, assigneeId, 1L);
    }

    private Project closedProject(ProjectStatus status) {
        return TestData.project(10L, status, reporter);
    }

    // ---------- create ----------

    @Test
    void createIsBlockedInAClosedProject() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(closedProject(ProjectStatus.COMPLETED)));

        assertThatThrownBy(() -> taskService.create(10L, createRequest(null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("COMPLETED");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createRejectsADueDateBeforeTheProjectStart() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() ->
                taskService.create(10L, createRequest(TestData.START.minusDays(1), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("before the project start date");
    }

    @Test
    void createRejectsAReporterWhoIsNotAMember() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(projectMemberRepository.existsByProjectIdAndUserId(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.create(10L, createRequest(null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Reporter is not a member of this project");

        verify(taskRepository, never()).save(any());
    }

    @Test
    void createRejectsAnInactiveAssignee() {
        User assignee = TestData.user(2L, "Bob Ray");
        assignee.deactivate();
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(projectMemberRepository.existsByProjectIdAndUserId(10L, 1L)).thenReturn(true);
        when(userRepository.findById(2L)).thenReturn(Optional.of(assignee));

        assertThatThrownBy(() -> taskService.create(10L, createRequest(null, 2L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Assignee is not an active user");
    }

    @Test
    void createSavesATodoTaskWithDefaultPriorityAndNoAssignee() {
        Task saved = TestData.task(20L, project, reporter);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(projectMemberRepository.existsByProjectIdAndUserId(10L, 1L)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenReturn(saved);
        when(taskMapper.toResponse(eq(saved), any(LocalDate.class))).thenReturn(responseFor(saved));

        taskService.create(10L, createRequest(null, null));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task toSave = captor.getValue();
        assertThat(toSave.getTitle()).isEqualTo("Fix bug");
        assertThat(toSave.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(toSave.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(toSave.getReporter()).isSameAs(reporter);
        assertThat(toSave.getProject()).isSameAs(project);
        assertThat(toSave.getAssignee()).isNull();
    }

    // ---------- status ----------

    @Test
    void updateStatusRejectsAnIllegalMove() {
        Task task = TestData.task(20L, project, reporter);
        when(taskRepository.findById(20L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() ->
                taskService.updateStatus(20L, new UpdateTaskStatusRequest(TaskStatus.DONE)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cannot change task status from TODO to DONE");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void updateStatusAcceptsALegalMove() {
        Task task = TestData.task(20L, project, reporter);
        when(taskRepository.findById(20L)).thenReturn(Optional.of(task));
        when(taskMapper.toResponse(eq(task), any(LocalDate.class))).thenReturn(responseFor(task));

        taskService.updateStatus(20L, new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS));

        assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void updateStatusIsBlockedInACancelledProject() {
        Task task = TestData.task(20L, closedProject(ProjectStatus.CANCELLED), reporter);
        when(taskRepository.findById(20L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() ->
                taskService.updateStatus(20L, new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED");

        assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
    }

    // ---------- assignee ----------

    @Test
    void sendingANullAssigneeUnassignsTheTask() {
        Task task = TestData.task(20L, project, reporter);
        task.setAssignee(reporter);
        when(taskRepository.findById(20L)).thenReturn(Optional.of(task));
        when(taskMapper.toResponse(eq(task), any(LocalDate.class))).thenReturn(responseFor(task));

        taskService.updateAssignee(20L, new UpdateTaskAssigneeRequest(null));

        assertThat(task.getAssignee()).isNull();
    }

    @Test
    void assigningAUserWhoIsNotAMemberIsRejected() {
        Task task = TestData.task(20L, project, reporter);
        User outsider = TestData.user(2L, "Bob Ray");
        when(taskRepository.findById(20L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(outsider));
        when(projectMemberRepository.existsByProjectIdAndUserId(10L, 2L)).thenReturn(false);

        assertThatThrownBy(() ->
                taskService.updateAssignee(20L, new UpdateTaskAssigneeRequest(2L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Assignee is not a member of this project");

        assertThat(task.getAssignee()).isNull();
    }

    // ---------- delete ----------

    @Test
    void deleteRemovesTheCommentsBeforeTheTask() {
        Task task = TestData.task(20L, project, reporter);
        when(taskRepository.findById(20L)).thenReturn(Optional.of(task));

        taskService.delete(20L);

        InOrder order = inOrder(commentRepository, taskRepository);
        order.verify(commentRepository).deleteByTaskId(20L);
        order.verify(taskRepository).deleteById(20L);
    }

    @Test
    void deleteIsBlockedInAClosedProject() {
        Task task = TestData.task(20L, closedProject(ProjectStatus.COMPLETED), reporter);
        when(taskRepository.findById(20L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.delete(20L))
                .isInstanceOf(BusinessRuleException.class);

        verifyNoInteractions(commentRepository);
        verify(taskRepository, never()).deleteById(any());
    }

    // ---------- overdue ----------

    @Test
    void overdueListRejectsAnUnknownSortField() {
        PageRequest pageable = PageRequest.of(0, 10, Sort.by("nonsense"));

        assertThatThrownBy(() -> taskService.getOverdue(pageable))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Invalid sort field 'nonsense'");

        verifyNoInteractions(taskRepository);
    }
}