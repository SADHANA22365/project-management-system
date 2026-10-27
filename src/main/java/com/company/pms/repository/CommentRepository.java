package com.company.pms.repository;

import com.company.pms.entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = "author")
    List<Comment> findByTaskIdOrderByCreatedAtAscIdAsc(Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Comment c where c.task.id = :taskId")
    void deleteByTaskId(@Param("taskId") Long taskId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from Comment c where c.task.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);
}