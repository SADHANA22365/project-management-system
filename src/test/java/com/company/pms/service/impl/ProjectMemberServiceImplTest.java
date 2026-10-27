package com.company.pms.service.impl;

import com.company.pms.dto.request.AddMemberRequest;
import com.company.pms.dto.request.UpdateMemberRoleRequest;
import com.company.pms.dto.response.MemberResponse;
import com.company.pms.entity.Project;
import com.company.pms.entity.ProjectMember;
import com.company.pms.entity.User;
import com.company.pms.enums.Designation;
import com.company.pms.enums.ProjectRole;
import com.company.pms.enums.ProjectStatus;
import com.company.pms.enums.TaskStatus;
import com.company.pms.exception.BusinessRuleException;
import com.company.pms.exception.DuplicateResourceException;
import com.company.pms.exception.ResourceNotFoundException;
import com.company.pms.mapper.ProjectMemberMapper;
import com.company.pms.repository.ProjectMemberRepository;
import com.company.pms.repository.ProjectRepository;
import com.company.pms.repository.TaskRepository;
import com.company.pms.repository.UserRepository;
import com.company.pms.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceImplTest {

    @Mock
    private ProjectMemberRepository memberRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ProjectMemberMapper memberMapper;

    @InjectMocks
    private ProjectMemberServiceImpl memberService;

    private User manager;
    private Project project;

    @BeforeEach
    void setUp() {
        manager = TestData.user(1L, "Ann Lee");
        project = TestData.project(10L, ProjectStatus.ACTIVE, manager);
    }

    private ProjectMember memberWithRole(Long userId, ProjectRole role) {
        User user = TestData.user(userId, "User " + userId);
        return TestData.withId(new ProjectMember(project, user, role), 100L + userId);
    }

    private static MemberResponse responseFor(ProjectMember member) {
        User user = member.getUser();
        return new MemberResponse(user.getId(), user.getFullName(), user.getEmail(),
                Designation.DEVELOPER, user.isActive(), member.getProjectRole(), LocalDate.now());
    }

    // ---------- addMember ----------

    @Test
    void addMemberThrowsNotFoundForAnUnknownUser() {
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                memberService.addMember(10L, new AddMemberRequest(2L, ProjectRole.MEMBER)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addMemberRejectsAnInactiveUser() {
        User inactive = TestData.user(2L, "Bob Ray");
        inactive.deactivate();
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() ->
                memberService.addMember(10L, new AddMemberRequest(2L, ProjectRole.MEMBER)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Inactive users");

        verify(memberRepository, never()).save(any());
    }

    @Test
    void addMemberRejectsAUserWhoIsAlreadyAMember() {
        User user = TestData.user(2L, "Bob Ray");
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(memberRepository.existsByProjectIdAndUserId(10L, 2L)).thenReturn(true);

        assertThatThrownBy(() ->
                memberService.addMember(10L, new AddMemberRequest(2L, ProjectRole.MEMBER)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(memberRepository, never()).save(any());
    }

    @Test
    void addMemberSavesANewMemberWithTheRequestedRole() {
        User user = TestData.user(2L, "Bob Ray");
        ProjectMember saved = TestData.withId(new ProjectMember(project, user, ProjectRole.REVIEWER), 102L);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(memberRepository.existsByProjectIdAndUserId(10L, 2L)).thenReturn(false);
        when(memberRepository.save(any(ProjectMember.class))).thenReturn(saved);
        when(memberMapper.toResponse(saved)).thenReturn(responseFor(saved));

        memberService.addMember(10L, new AddMemberRequest(2L, ProjectRole.REVIEWER));

        ArgumentCaptor<ProjectMember> captor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(memberRepository).save(captor.capture());
        assertThat(captor.getValue().getProjectRole()).isEqualTo(ProjectRole.REVIEWER);
        assertThat(captor.getValue().getUser()).isSameAs(user);
        assertThat(captor.getValue().getProject()).isSameAs(project);
    }

    // ---------- updateRole ----------

    @Test
    void theOnlyLeadCannotBeDemoted() {
        ProjectMember lead = memberWithRole(1L, ProjectRole.LEAD);
        when(memberRepository.findByProjectIdAndUserId(10L, 1L)).thenReturn(Optional.of(lead));
        when(memberRepository.countByProjectIdAndProjectRole(10L, ProjectRole.LEAD)).thenReturn(1L);

        assertThatThrownBy(() ->
                memberService.updateRole(10L, 1L, new UpdateMemberRoleRequest(ProjectRole.MEMBER)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("A project must always have at least one LEAD");

        assertThat(lead.getProjectRole()).isEqualTo(ProjectRole.LEAD);
    }

    @Test
    void aLeadCanBeDemotedWhenAnotherLeadExists() {
        ProjectMember lead = memberWithRole(1L, ProjectRole.LEAD);
        when(memberRepository.findByProjectIdAndUserId(10L, 1L)).thenReturn(Optional.of(lead));
        when(memberRepository.countByProjectIdAndProjectRole(10L, ProjectRole.LEAD)).thenReturn(2L);
        when(memberMapper.toResponse(lead)).thenReturn(responseFor(lead));

        memberService.updateRole(10L, 1L, new UpdateMemberRoleRequest(ProjectRole.MEMBER));

        assertThat(lead.getProjectRole()).isEqualTo(ProjectRole.MEMBER);
    }

    @Test
    void promotingToLeadNeverChecksTheLeadCount() {
        ProjectMember member = memberWithRole(2L, ProjectRole.MEMBER);
        when(memberRepository.findByProjectIdAndUserId(10L, 2L)).thenReturn(Optional.of(member));
        when(memberMapper.toResponse(member)).thenReturn(responseFor(member));

        memberService.updateRole(10L, 2L, new UpdateMemberRoleRequest(ProjectRole.LEAD));

        assertThat(member.getProjectRole()).isEqualTo(ProjectRole.LEAD);
        verify(memberRepository, never()).countByProjectIdAndProjectRole(anyLong(), any());
    }

    // ---------- removeMember ----------

    @Test
    void removeThrowsNotFoundForSomeoneWhoIsNotAMember() {
        when(memberRepository.findByProjectIdAndUserId(10L, 9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.removeMember(10L, 9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void theOnlyLeadCannotBeRemoved() {
        ProjectMember lead = memberWithRole(1L, ProjectRole.LEAD);
        when(memberRepository.findByProjectIdAndUserId(10L, 1L)).thenReturn(Optional.of(lead));
        when(memberRepository.countByProjectIdAndProjectRole(10L, ProjectRole.LEAD)).thenReturn(1L);

        assertThatThrownBy(() -> memberService.removeMember(10L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at least one LEAD");

        verify(memberRepository, never()).delete(any());
    }

    @Test
    void aMemberWithOpenTasksCannotBeRemoved() {
        ProjectMember member = memberWithRole(2L, ProjectRole.MEMBER);
        when(memberRepository.findByProjectIdAndUserId(10L, 2L)).thenReturn(Optional.of(member));
        when(taskRepository.existsByAssigneeIdAndProjectIdAndStatusNot(2L, 10L, TaskStatus.DONE))
                .thenReturn(true);

        assertThatThrownBy(() -> memberService.removeMember(10L, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("open tasks");

        verify(memberRepository, never()).delete(any());
    }

    @Test
    void aMemberWithNoOpenTasksIsRemoved() {
        ProjectMember member = memberWithRole(2L, ProjectRole.MEMBER);
        when(memberRepository.findByProjectIdAndUserId(10L, 2L)).thenReturn(Optional.of(member));
        when(taskRepository.existsByAssigneeIdAndProjectIdAndStatusNot(2L, 10L, TaskStatus.DONE))
                .thenReturn(false);

        memberService.removeMember(10L, 2L);

        verify(memberRepository).delete(eq(member));
    }
}