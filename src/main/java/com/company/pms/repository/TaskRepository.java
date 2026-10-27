package com.company.pms.repository;

import com.company.pms.entity.Task;
import com.company.pms.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = {"assignee", "reporter"})
    Page<Task> findByProjectId(Long projectId, Pageable pageable);

    @EntityGraph(attributePaths = {"assignee", "reporter"})
    Page<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"project", "assignee", "reporter"})
    @Query("select t from Task t where t.dueDate < :today and t.status <> com.company.pms.enums.TaskStatus.DONE")
    Page<Task> findOverdue(@Param("today") LocalDate today, Pageable pageable);

    boolean existsByAssigneeIdAndProjectIdAndStatusNot(Long assigneeId, Long projectId, TaskStatus status);

    long countByProjectId(Long projectId);

    long countByProjectIdAndStatusNot(Long projectId, TaskStatus status);

    long countByLabelsId(Long labelId);

    @Query("select t.status as status, count(t) as total "
            + "from Task t where t.project.id = :projectId group by t.status")
    List<TaskStatusCount> countByStatusForProject(@Param("projectId") Long projectId);

    @Query("select t.status as status, count(t) as total from Task t group by t.status")
    List<TaskStatusCount> countGroupedByStatus();

    @Query("select count(t) from Task t "
            + "where t.dueDate < :today and t.status <> com.company.pms.enums.TaskStatus.DONE")
    long countOverdue(@Param("today") LocalDate today);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "delete from task_labels where task_id in "
            + "(select id from tasks where project_id = :projectId)", nativeQuery = true)
    void deleteLabelLinksByProjectId(@Param("projectId") Long projectId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Task t where t.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);
}