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
}