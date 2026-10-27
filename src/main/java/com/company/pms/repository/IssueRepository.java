package com.company.pms.repository;

import com.company.pms.entity.Issue;
import com.company.pms.enums.IssueSeverity;
import com.company.pms.enums.IssueStatus;
import com.company.pms.enums.IssueType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    @EntityGraph(attributePaths = {"assignee", "reporter"})
    @Query("select i from Issue i where i.project.id = :projectId "
            + "and (:status is null or i.status = :status) "
            + "and (:type is null or i.type = :type) "
            + "and (:severity is null or i.severity = :severity)")
    Page<Issue> search(@Param("projectId") Long projectId,
                       @Param("status") IssueStatus status,
                       @Param("type") IssueType type,
                       @Param("severity") IssueSeverity severity,
                       Pageable pageable);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Issue i where i.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);

    @Query("select i.status as status, i.type as type, i.severity as severity, count(i) as total "
            + "from Issue i where i.project.id = :projectId "
            + "group by i.status, i.type, i.severity")
    List<IssueCount> countGroupedByProject(@Param("projectId") Long projectId);

    @Query("select i.assignee.id as assigneeId, i.assignee.fullName as assigneeName, count(i) as total "
            + "from Issue i where i.project.id = :projectId "
            + "and i.type = com.company.pms.enums.IssueType.BUG "
            + "and i.status in :statuses "
            + "group by i.assignee.id, i.assignee.fullName "
            + "order by count(i) desc, i.assignee.id asc")
    List<OpenBugCount> countOpenBugsByAssignee(@Param("projectId") Long projectId,
                                               @Param("statuses") Collection<IssueStatus> statuses);
}