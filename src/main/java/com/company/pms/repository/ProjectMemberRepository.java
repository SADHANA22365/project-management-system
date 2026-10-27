package com.company.pms.repository;

import com.company.pms.entity.ProjectMember;
import com.company.pms.enums.ProjectRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    long countByProjectIdAndProjectRole(Long projectId, ProjectRole projectRole);

    @EntityGraph(attributePaths = "user")
    List<ProjectMember> findByProjectIdOrderByJoinedOnAscIdAsc(Long projectId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ProjectMember m where m.project.id = :projectId")
    void deleteByProjectId(@Param("projectId") Long projectId);

    long countByProjectId(Long projectId);
}