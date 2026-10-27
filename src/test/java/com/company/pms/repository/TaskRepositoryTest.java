package com.company.pms.repository;

import com.company.pms.entity.Project;
import com.company.pms.entity.User;
import com.company.pms.enums.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class TaskRepositoryTest extends RepositoryTestBase {

    private User ann;
    private User bob;
    private Project alpha;
    private Project beta;

    @BeforeEach
    void setUp() {
        ann = saveUser("Ann Lee");
        bob = saveUser("Bob Ray");
        alpha = saveProject("Alpha", ann);
        beta = saveProject("Beta", ann);

        // alpha
        saveTask(alpha, ann, bob, TaskStatus.TODO, TODAY.minusDays(1));        // overdue
        saveTask(alpha, ann, ann, TaskStatus.IN_PROGRESS, TODAY.minusDays(1)); // overdue
        saveTask(alpha, ann, bob, TaskStatus.DONE, TODAY.minusDays(1));        // done, not overdue
        saveTask(alpha, ann, null, TaskStatus.TODO, TODAY.plusDays(1));        // in the future
        saveTask(alpha, ann, null, TaskStatus.TODO, null);                     // no due date
        // beta
        saveTask(beta, ann, bob, TaskStatus.BLOCKED, TODAY.minusDays(1));      // overdue
    }

    private static Map<TaskStatus, Long> asMap(List<TaskStatusCount> rows) {
        return rows.stream().collect(
                Collectors.toMap(TaskStatusCount::getStatus, TaskStatusCount::getTotal));
    }

    @Test
    void countByStatusForProjectGroupsOnlyThatProjectsTasks() {
        Map<TaskStatus, Long> result = asMap(taskRepository.countByStatusForProject(alpha.getId()));

        assertThat(result).isEqualTo(Map.of(
                TaskStatus.TODO, 3L,
                TaskStatus.IN_PROGRESS, 1L,
                TaskStatus.DONE, 1L));
    }

    @Test
    void countGroupedByStatusCoversEveryProject() {
        Map<TaskStatus, Long> result = asMap(taskRepository.countGroupedByStatus());

        assertThat(result).isEqualTo(Map.of(
                TaskStatus.TODO, 3L,
                TaskStatus.IN_PROGRESS, 1L,
                TaskStatus.DONE, 1L,
                TaskStatus.BLOCKED, 1L));
    }

    @Test
    void overdueMeansPastDueAndNotDone() {
        assertThat(taskRepository.countOverdue(TODAY)).isEqualTo(3);
        assertThat(taskRepository.countOverdueByProjectId(alpha.getId(), TODAY)).isEqualTo(2);
        assertThat(taskRepository.countOverdueByProjectId(beta.getId(), TODAY)).isEqualTo(1);
    }

    @Test
    void overdueCanBeCountedPerAssignee() {
        assertThat(taskRepository.countOverdueByAssigneeId(bob.getId(), TODAY)).isEqualTo(2);
        assertThat(taskRepository.countOverdueByAssigneeId(ann.getId(), TODAY)).isEqualTo(1);
    }

    @Test
    void countByAssigneeGroupedByStatusCoversAllProjects() {
        Map<TaskStatus, Long> result =
                asMap(taskRepository.countByAssigneeGroupedByStatus(bob.getId()));

        assertThat(result).isEqualTo(Map.of(
                TaskStatus.TODO, 1L,
                TaskStatus.DONE, 1L,
                TaskStatus.BLOCKED, 1L));
    }

    @Test
    void openTaskCheckLooksAtTheAssigneeAndProject() {
        User cara = saveUser("Cara Doe");

        assertThat(taskRepository.existsByAssigneeIdAndProjectIdAndStatusNot(
                bob.getId(), alpha.getId(), TaskStatus.DONE)).isTrue();
        assertThat(taskRepository.existsByAssigneeIdAndProjectIdAndStatusNot(
                bob.getId(), beta.getId(), TaskStatus.DONE)).isTrue();
        assertThat(taskRepository.existsByAssigneeIdAndProjectIdAndStatusNot(
                cara.getId(), alpha.getId(), TaskStatus.DONE)).isFalse();
    }

    @Test
    void deleteByProjectIdRemovesOnlyThatProjectsTasks() {
        taskRepository.deleteByProjectId(alpha.getId());

        assertThat(taskRepository.countByProjectId(alpha.getId())).isZero();
        assertThat(taskRepository.countByProjectId(beta.getId())).isEqualTo(1);
    }
}