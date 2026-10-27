package com.company.pms.service.impl;

import com.company.pms.dto.request.CreateIssueRequest;
import com.company.pms.dto.request.UpdateIssueAssigneeRequest;
import com.company.pms.dto.request.UpdateIssueStatusRequest;
import com.company.pms.dto.response.IssueResponse;
import com.company.pms.dto.response.IssueStatisticsResponse;
import com.company.pms.dto.response.PageResponse;
import com.company.pms.entity.Issue;
import com.company.pms.entity.Project;
import com.company.pms.entity.User;
import com.company.pms.enums.IssueSeverity;
import com.company.pms.enums.IssueStatus;
import com.company.pms.enums.IssueType;
import com.company.pms.enums.ProjectStatus;
import com.company.pms.exception.BusinessRuleException;
import com.company.pms.exception.InvalidRequestException;
import com.company.pms.exception.ResourceNotFoundException;
import com.company.pms.mapper.IssueMapper;
import com.company.pms.repository.IssueCount;
import com.company.pms.repository.IssueRepository;
import com.company.pms.repository.OpenBugCount;
import com.company.pms.repository.ProjectMemberRepository;
import com.company.pms.repository.ProjectRepository;
import com.company.pms.repository.UserRepository;
import com.company.pms.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueServiceImplTest {

    @Mock
    private IssueRepository issueRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProjectMemberRepository projectMemberRepository;
    @Mock
    private IssueMapper issueMapper;

    @InjectMocks
    private IssueServiceImpl issueService;

    private User reporter;
    private Project project;

    @BeforeEach
    void setUp() {
        reporter = TestData.user(1L, "Ann Lee");
        project = TestData.project(10L, ProjectStatus.ACTIVE, reporter);
    }

    private Issue issue(Project inProject) {
        return TestData.withId(
                new Issue("Login crash", "details", IssueType.BUG, null, inProject, reporter, null), 40L);
    }

    private static IssueResponse responseFor(Issue issue) {
        return new IssueResponse(issue.getId(), issue.getTitle(), issue.getDescription(),
                issue.getType(), issue.getSeverity(), issue.getStatus(), issue.getResolvedAt(),
                10L, null, null, 1L, "Ann Lee", null, null);
    }

    private static IssueCount row(IssueStatus status, IssueType type, IssueSeverity severity, long total) {
        return new IssueCount() {
            @Override
            public IssueStatus getStatus() {
                return status;
            }

            @Override
            public IssueType getType() {
                return type;
            }

            @Override
            public IssueSeverity getSeverity() {
                return severity;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    private static OpenBugCount openBugs(Long assigneeId, String name, long total) {
        return new OpenBugCount() {
            @Override
            public Long getAssigneeId() {
                return assigneeId;
            }

            @Override
            public String getAssigneeName() {
                return name;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    // ---------- create ----------

    @Test
    void createIsBlockedInAClosedProject() {
        Project closed = TestData.project(10L, ProjectStatus.CANCELLED, reporter);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> issueService.create(10L,
                new CreateIssueRequest("Crash", null, IssueType.BUG, null, null, 1L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("CANCELLED");

        verify(issueRepository, never()).save(any());
    }

    @Test
    void createRejectsAReporterWhoIsNotAMember() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(projectMemberRepository.existsByProjectIdAndUserId(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> issueService.create(10L,
                new CreateIssueRequest("Crash", null, IssueType.BUG, null, null, 1L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Reporter is not a member of this project");
    }

    @Test
    void createSavesAnOpenIssueWithATrimmedTitleAndDefaultSeverity() {
        Issue saved = issue(project);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(1L)).thenReturn(Optional.of(reporter));
        when(projectMemberRepository.existsByProjectIdAndUserId(10L, 1L)).thenReturn(true);
        when(issueRepository.save(any(Issue.class))).thenReturn(saved);
        when(issueMapper.toResponse(saved)).thenReturn(responseFor(saved));

        issueService.create(10L,
                new CreateIssueRequest("  Login crash  ", "details", IssueType.BUG, null, null, 1L));

        ArgumentCaptor<Issue> captor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(captor.capture());
        Issue toSave = captor.getValue();
        assertThat(toSave.getTitle()).isEqualTo("Login crash");
        assertThat(toSave.getStatus()).isEqualTo(IssueStatus.OPEN);
        assertThat(toSave.getSeverity()).isEqualTo(IssueSeverity.MINOR);
        assertThat(toSave.getType()).isEqualTo(IssueType.BUG);
        assertThat(toSave.getReporter()).isSameAs(reporter);
        assertThat(toSave.getAssignee()).isNull();
    }

    // ---------- status ----------

    @Test
    void updateStatusRejectsAnIllegalMoveAndListsTheAllowedOnes() {
        Issue issue = issue(project);
        when(issueRepository.findById(40L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() ->
                issueService.updateStatus(40L, new UpdateIssueStatusRequest(IssueStatus.RESOLVED)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Cannot change issue status from OPEN to RESOLVED")
                .hasMessageContaining("IN_PROGRESS");

        assertThat(issue.getStatus()).isEqualTo(IssueStatus.OPEN);
        verify(issueRepository, never()).saveAndFlush(any());
    }

    @Test
    void resolvingAnIssueSetsResolvedAt() {
        Issue issue = issue(project);
        issue.changeStatus(IssueStatus.IN_PROGRESS, LocalDateTime.now());
        when(issueRepository.findById(40L)).thenReturn(Optional.of(issue));
        when(issueRepository.saveAndFlush(issue)).thenReturn(issue);
        when(issueMapper.toResponse(issue)).thenReturn(responseFor(issue));

        issueService.updateStatus(40L, new UpdateIssueStatusRequest(IssueStatus.RESOLVED));

        assertThat(issue.getStatus()).isEqualTo(IssueStatus.RESOLVED);
        assertThat(issue.getResolvedAt()).isNotNull();
    }

    @Test
    void reopeningAnIssueClearsResolvedAt() {
        Issue issue = issue(project);
        issue.changeStatus(IssueStatus.RESOLVED, LocalDateTime.now());
        when(issueRepository.findById(40L)).thenReturn(Optional.of(issue));
        when(issueRepository.saveAndFlush(issue)).thenReturn(issue);
        when(issueMapper.toResponse(issue)).thenReturn(responseFor(issue));

        issueService.updateStatus(40L, new UpdateIssueStatusRequest(IssueStatus.REOPENED));

        assertThat(issue.getStatus()).isEqualTo(IssueStatus.REOPENED);
        assertThat(issue.getResolvedAt()).isNull();
    }

    @Test
    void updateStatusIsBlockedInAClosedProject() {
        Project closed = TestData.project(10L, ProjectStatus.COMPLETED, reporter);
        Issue issue = issue(closed);
        when(issueRepository.findById(40L)).thenReturn(Optional.of(issue));

        assertThatThrownBy(() ->
                issueService.updateStatus(40L, new UpdateIssueStatusRequest(IssueStatus.IN_PROGRESS)))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(issue.getStatus()).isEqualTo(IssueStatus.OPEN);
    }

    // ---------- assignee ----------

    @Test
    void sendingANullAssigneeUnassignsTheIssue() {
        Issue issue = issue(project);
        issue.setAssignee(reporter);
        when(issueRepository.findById(40L)).thenReturn(Optional.of(issue));
        when(issueRepository.saveAndFlush(issue)).thenReturn(issue);
        when(issueMapper.toResponse(issue)).thenReturn(responseFor(issue));

        issueService.updateAssignee(40L, new UpdateIssueAssigneeRequest(null));

        assertThat(issue.getAssignee()).isNull();
    }

    @Test
    void assigningAUserWhoIsNotAMemberIsRejected() {
        Issue issue = issue(project);
        User outsider = TestData.user(2L, "Bob Ray");
        when(issueRepository.findById(40L)).thenReturn(Optional.of(issue));
        when(userRepository.findById(2L)).thenReturn(Optional.of(outsider));
        when(projectMemberRepository.existsByProjectIdAndUserId(10L, 2L)).thenReturn(false);

        assertThatThrownBy(() ->
                issueService.updateAssignee(40L, new UpdateIssueAssigneeRequest(2L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Assignee is not a member of this project");

        assertThat(issue.getAssignee()).isNull();
    }

    // ---------- list ----------

    @Test
    void listThrowsNotFoundForAMissingProject() {
        when(projectRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() ->
                issueService.getByProject(99L, null, null, null, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(issueRepository);
    }

    @Test
    void listRejectsAnUnknownSortField() {
        when(projectRepository.existsById(10L)).thenReturn(true);

        assertThatThrownBy(() -> issueService.getByProject(
                10L, null, null, null, PageRequest.of(0, 10, Sort.by("titel"))))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Invalid sort field 'titel'");

        verifyNoInteractions(issueRepository);
    }

    @Test
    void listPassesTheFiltersToTheRepository() {
        Issue issue = issue(project);
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));
        when(projectRepository.existsById(10L)).thenReturn(true);
        when(issueRepository.search(10L, IssueStatus.OPEN, IssueType.BUG, null, pageable))
                .thenReturn(new PageImpl<>(List.of(issue), pageable, 1));
        when(issueMapper.toResponse(issue)).thenReturn(responseFor(issue));

        PageResponse<IssueResponse> result =
                issueService.getByProject(10L, IssueStatus.OPEN, IssueType.BUG, null, pageable);

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    // ---------- statistics ----------

    @Test
    void statisticsThrowsNotFoundForAMissingProject() {
        when(projectRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> issueService.getStatistics(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void statisticsAddsUpTheGroupedRowsInThreeWays() {
        when(projectRepository.existsById(10L)).thenReturn(true);
        when(issueRepository.countGroupedByProject(10L)).thenReturn(List.of(
                row(IssueStatus.OPEN, IssueType.BUG, IssueSeverity.MAJOR, 2),
                row(IssueStatus.IN_PROGRESS, IssueType.BUG, IssueSeverity.MINOR, 1),
                row(IssueStatus.RESOLVED, IssueType.BUG, IssueSeverity.MAJOR, 3),
                row(IssueStatus.OPEN, IssueType.IMPROVEMENT, IssueSeverity.MINOR, 4),
                row(IssueStatus.CLOSED, IssueType.SUPPORT, IssueSeverity.MINOR, 1)));
        when(issueRepository.countOpenBugsByAssignee(eq(10L),
                eq(List.of(IssueStatus.OPEN, IssueStatus.IN_PROGRESS, IssueStatus.REOPENED))))
                .thenReturn(List.of(openBugs(1L, "Ann Lee", 2)));

        IssueStatisticsResponse stats = issueService.getStatistics(10L);

        assertThat(stats.totalIssues()).isEqualTo(11);
        assertThat(stats.openIssues()).isEqualTo(7);
        assertThat(stats.openBugs()).isEqualTo(3);
        assertThat(stats.byStatus()).containsEntry(IssueStatus.OPEN, 6L)
                .containsEntry(IssueStatus.IN_PROGRESS, 1L)
                .containsEntry(IssueStatus.RESOLVED, 3L)
                .containsEntry(IssueStatus.CLOSED, 1L)
                .containsEntry(IssueStatus.REOPENED, 0L);
        assertThat(stats.byType()).containsEntry(IssueType.BUG, 6L)
                .containsEntry(IssueType.IMPROVEMENT, 4L)
                .containsEntry(IssueType.SUPPORT, 1L)
                .containsEntry(IssueType.FEATURE_REQUEST, 0L);
        assertThat(stats.bySeverity()).containsEntry(IssueSeverity.MAJOR, 5L)
                .containsEntry(IssueSeverity.MINOR, 6L)
                .containsEntry(IssueSeverity.CRITICAL, 0L)
                .containsEntry(IssueSeverity.BLOCKER, 0L);
        assertThat(stats.openBugsByAssignee()).hasSize(1);
        assertThat(stats.openBugsByAssignee().get(0).assigneeName()).isEqualTo("Ann Lee");
        assertThat(stats.openBugsByAssignee().get(0).openBugs()).isEqualTo(2);
    }
}