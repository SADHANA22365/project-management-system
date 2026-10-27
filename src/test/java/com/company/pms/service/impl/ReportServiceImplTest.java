package com.company.pms.service.impl;

import com.company.pms.dto.response.DashboardResponse;
import com.company.pms.dto.response.ProjectProgressResponse;
import com.company.pms.dto.response.ProjectSummaryResponse;
import com.company.pms.dto.response.UserWorkloadResponse;
import com.company.pms.entity.Project;
import com.company.pms.entity.User;
import com.company.pms.enums.ProjectStatus;
import com.company.pms.enums.TaskStatus;
import com.company.pms.exception.ResourceNotFoundException;
import com.company.pms.repository.ProjectMemberRepository;
import com.company.pms.repository.ProjectRepository;
import com.company.pms.repository.ProjectStatusCount;
import com.company.pms.repository.TaskRepository;
import com.company.pms.repository.TaskStatusCount;
import com.company.pms.repository.UserRepository;
import com.company.pms.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private User manager;
    private Project project;

    @BeforeEach
    void setUp() {
        manager = TestData.user(1L, "Ann Lee");
        project = TestData.project(10L, ProjectStatus.ACTIVE, manager);
    }

    private static TaskStatusCount taskRow(TaskStatus status, long total) {
        return new TaskStatusCount() {
            @Override
            public TaskStatus getStatus() {
                return status;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    private static ProjectStatusCount projectRow(ProjectStatus status, long total) {
        return new ProjectStatusCount() {
            @Override
            public ProjectStatus getStatus() {
                return status;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    // ---------- progress ----------

    @Test
    void progressThrowsNotFoundForAMissingProject() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getProjectProgress(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project not found with id 99");
    }

    @Test
    void aProjectWithNoTasksHasZeroProgressAndAllStatusesPresent() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(taskRepository.countByStatusForProject(10L)).thenReturn(List.of());

        ProjectProgressResponse result = reportService.getProjectProgress(10L);

        assertThat(result.totalTasks()).isZero();
        assertThat(result.doneTasks()).isZero();
        assertThat(result.progressPercent()).isEqualTo(0.0);
        assertThat(result.tasksByStatus()).hasSize(TaskStatus.values().length);
        assertThat(result.tasksByStatus().values()).containsOnly(0L);
    }

    @ParameterizedTest(name = "{0} done out of {1} tasks is {2} percent")
    @CsvSource({
            "1, 3, 33.3",
            "2, 3, 66.7",
            "0, 5, 0.0",
            "5, 5, 100.0",
            "1, 8, 12.5",
            "1, 7, 14.3"
    })
    void progressIsRoundedToOneDecimalPlace(long done, long total, double expected) {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(taskRepository.countByStatusForProject(10L)).thenReturn(List.of(
                taskRow(TaskStatus.DONE, done),
                taskRow(TaskStatus.TODO, total - done)));

        ProjectProgressResponse result = reportService.getProjectProgress(10L);

        assertThat(result.totalTasks()).isEqualTo(total);
        assertThat(result.doneTasks()).isEqualTo(done);
        assertThat(result.progressPercent()).isEqualTo(expected);
    }

    // ---------- dashboard ----------

    @Test
    void dashboardFillsMissingStatusesAndSumsTheTotals() {
        when(projectRepository.countGroupedByStatus()).thenReturn(List.of(
                projectRow(ProjectStatus.ACTIVE, 2),
                projectRow(ProjectStatus.COMPLETED, 1)));
        when(taskRepository.countGroupedByStatus()).thenReturn(List.of(taskRow(TaskStatus.TODO, 4)));
        when(taskRepository.countOverdue(any(LocalDate.class))).thenReturn(2L);
        when(userRepository.count()).thenReturn(5L);
        when(userRepository.countByActiveTrue()).thenReturn(4L);

        DashboardResponse result = reportService.getDashboard();

        assertThat(result.totalProjects()).isEqualTo(3);
        assertThat(result.projectsByStatus()).containsEntry(ProjectStatus.ACTIVE, 2L)
                .containsEntry(ProjectStatus.COMPLETED, 1L)
                .containsEntry(ProjectStatus.PLANNED, 0L)
                .containsEntry(ProjectStatus.CANCELLED, 0L);
        assertThat(result.totalTasks()).isEqualTo(4);
        assertThat(result.tasksByStatus()).containsEntry(TaskStatus.TODO, 4L)
                .containsEntry(TaskStatus.DONE, 0L);
        assertThat(result.overdueTasks()).isEqualTo(2);
        assertThat(result.totalUsers()).isEqualTo(5);
        assertThat(result.activeUsers()).isEqualTo(4);
    }

    // ---------- summary ----------

    @Test
    void summaryCombinesProgressMembersAndOverdueTasks() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(taskRepository.countByStatusForProject(10L)).thenReturn(List.of(
                taskRow(TaskStatus.DONE, 1), taskRow(TaskStatus.TODO, 1)));
        when(projectMemberRepository.countByProjectId(10L)).thenReturn(3L);
        when(taskRepository.countOverdueByProjectId(eq(10L), any(LocalDate.class))).thenReturn(1L);

        ProjectSummaryResponse result = reportService.getProjectSummary(10L);

        assertThat(result.projectId()).isEqualTo(10L);
        assertThat(result.status()).isEqualTo(ProjectStatus.ACTIVE);
        assertThat(result.managerName()).isEqualTo("Ann Lee");
        assertThat(result.memberCount()).isEqualTo(3);
        assertThat(result.overdueTasks()).isEqualTo(1);
        assertThat(result.progress().progressPercent()).isEqualTo(50.0);
    }

    @Test
    void summaryThrowsNotFoundForAMissingProject() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getProjectSummary(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- workload ----------

    @Test
    void workloadCountsOpenTasksAsEverythingAssignedExceptDone() {
        User user = TestData.user(2L, "Bob Ray");
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(taskRepository.countByAssigneeGroupedByStatus(2L)).thenReturn(List.of(
                taskRow(TaskStatus.TODO, 2),
                taskRow(TaskStatus.IN_PROGRESS, 1),
                taskRow(TaskStatus.DONE, 4)));
        when(taskRepository.countOverdueByAssigneeId(eq(2L), any(LocalDate.class))).thenReturn(1L);

        UserWorkloadResponse result = reportService.getUserWorkload(2L);

        assertThat(result.userName()).isEqualTo("Bob Ray");
        assertThat(result.openTasks()).isEqualTo(3);
        assertThat(result.overdueTasks()).isEqualTo(1);
        assertThat(result.assignedByStatus()).containsEntry(TaskStatus.DONE, 4L)
                .containsEntry(TaskStatus.BLOCKED, 0L);
    }

    @Test
    void workloadThrowsNotFoundForAMissingUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.getUserWorkload(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id 99");
    }
}