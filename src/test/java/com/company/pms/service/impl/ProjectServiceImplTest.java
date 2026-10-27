package com.company.pms.service.impl;

import com.company.pms.dto.request.CreateProjectRequest;
import com.company.pms.dto.request.UpdateProjectStatusRequest;
import com.company.pms.dto.response.ProjectResponse;
import com.company.pms.entity.Project;
import com.company.pms.entity.ProjectMember;
import com.company.pms.entity.User;
import com.company.pms.enums.ProjectRole;
import com.company.pms.enums.ProjectStatus;
import com.company.pms.enums.TaskStatus;
import com.company.pms.exception.BusinessRuleException;
import com.company.pms.exception.DuplicateResourceException;
import com.company.pms.exception.ResourceNotFoundException;
import com.company.pms.mapper.ProjectMapper;
import com.company.pms.repository.CommentRepository;
import com.company.pms.repository.IssueRepository;
import com.company.pms.repository.ProjectMemberRepository;
import com.company.pms.repository.ProjectRepository;
import com.company.pms.repository.TaskRepository;
import com.company.pms.repository.UserRepository;
import com.company.pms.support.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectMemberRepository memberRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private IssueRepository issueRepository;
    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private ProjectServiceImpl projectService;

    private static CreateProjectRequest createRequest(LocalDate start, LocalDate end) {
        return new CreateProjectRequest("Alpha", null, start, end, 1L);
    }

    private static ProjectResponse responseFor(Project project) {
        return new ProjectResponse(project.getId(), project.getName(), null,
                project.getStartDate(), project.getEndDate(), project.getStatus(),
                1L, "Ann Lee", null, null);
    }

    // ---------- create ----------

    @Test
    void createRejectsADuplicateName() {
        when(projectRepository.existsByNameIgnoreCase("Alpha")).thenReturn(true);

        assertThatThrownBy(() -> projectService.create(createRequest(TestData.START, null)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Alpha");

        verify(projectRepository, never()).save(any());
    }

    @Test
    void createRejectsAnEndDateBeforeTheStartDate() {
        when(projectRepository.existsByNameIgnoreCase("Alpha")).thenReturn(false);

        CreateProjectRequest request = createRequest(TestData.START, TestData.START.minusDays(1));

        assertThatThrownBy(() -> projectService.create(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("End date");
    }

    @Test
    void createThrowsNotFoundWhenTheManagerDoesNotExist() {
        when(projectRepository.existsByNameIgnoreCase("Alpha")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.create(createRequest(TestData.START, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createRejectsAnInactiveManager() {
        User manager = TestData.user(1L, "Ann Lee");
        manager.deactivate();
        when(projectRepository.existsByNameIgnoreCase("Alpha")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> projectService.create(createRequest(TestData.START, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active");
    }

    @Test
    void createSavesTheProjectAndMakesTheManagerALead() {
        User manager = TestData.user(1L, "Ann Lee");
        CreateProjectRequest request = createRequest(TestData.START, null);
        Project entity = TestData.project(null, ProjectStatus.PLANNED, manager);
        Project saved = TestData.project(7L, ProjectStatus.PLANNED, manager);
        ProjectResponse expected = responseFor(saved);

        when(projectRepository.existsByNameIgnoreCase("Alpha")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(projectMapper.toEntity(request, manager)).thenReturn(entity);
        when(projectRepository.save(entity)).thenReturn(saved);
        when(projectMapper.toResponse(saved)).thenReturn(expected);

        ProjectResponse result = projectService.create(request);

        assertThat(result).isSameAs(expected);
        ArgumentCaptor<ProjectMember> captor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(memberRepository).save(captor.capture());
        ProjectMember member = captor.getValue();
        assertThat(member.getProject()).isSameAs(saved);
        assertThat(member.getUser()).isSameAs(manager);
        assertThat(member.getProjectRole()).isEqualTo(ProjectRole.LEAD);
    }

    // ---------- status ----------

    @Test
    void updateStatusRejectsAnIllegalMove() {
        Project project = TestData.project(1L, ProjectStatus.PLANNED, TestData.user(1L, "Ann Lee"));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() ->
                projectService.updateStatus(1L, new UpdateProjectStatusRequest(ProjectStatus.COMPLETED)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot change project status");

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.PLANNED);
    }

    @Test
    void completingIsBlockedWhileTasksAreNotDone() {
        Project project = TestData.project(1L, ProjectStatus.ACTIVE, TestData.user(1L, "Ann Lee"));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.countByProjectIdAndStatusNot(1L, TaskStatus.DONE)).thenReturn(2L);

        assertThatThrownBy(() ->
                projectService.updateStatus(1L, new UpdateProjectStatusRequest(ProjectStatus.COMPLETED)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not DONE");

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.ACTIVE);
    }

    @Test
    void completingWorksWhenEveryTaskIsDone() {
        Project project = TestData.project(1L, ProjectStatus.ACTIVE, TestData.user(1L, "Ann Lee"));
        ProjectResponse expected = responseFor(project);
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(taskRepository.countByProjectIdAndStatusNot(1L, TaskStatus.DONE)).thenReturn(0L);
        when(projectMapper.toResponse(project)).thenReturn(expected);

        projectService.updateStatus(1L, new UpdateProjectStatusRequest(ProjectStatus.COMPLETED));

        assertThat(project.getStatus()).isEqualTo(ProjectStatus.COMPLETED);
    }

    // ---------- delete ----------

    @Test
    void deleteRemovesDependentRowsBeforeTheProject() {
        Project project = TestData.project(1L, ProjectStatus.ACTIVE, TestData.user(1L, "Ann Lee"));
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        projectService.delete(1L);

        InOrder order = inOrder(commentRepository, issueRepository, taskRepository,
                memberRepository, projectRepository);
        order.verify(commentRepository).deleteByProjectId(1L);
        order.verify(issueRepository).deleteByProjectId(1L);
        order.verify(taskRepository).deleteLabelLinksByProjectId(1L);
        order.verify(taskRepository).deleteByProjectId(1L);
        order.verify(memberRepository).deleteByProjectId(1L);
        order.verify(projectRepository).deleteById(1L);
    }

    @Test
    void deleteThrowsNotFoundAndDeletesNothingForAMissingProject() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(commentRepository, issueRepository, taskRepository, memberRepository);
        verify(projectRepository, never()).deleteById(any());
    }
}