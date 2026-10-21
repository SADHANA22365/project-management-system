package com.company.pms.repository;

import com.company.pms.entity.ProjectMember;
import com.company.pms.enums.ProjectRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    boolean existsByProjectIdAndUserId(Long projectId, Long userId);

    Optional<ProjectMember> findByProjectIdAndUserId(Long projectId, Long userId);

    long countByProjectIdAndProjectRole(Long projectId, ProjectRole projectRole);

    @EntityGraph(attributePaths = "user")
    List<ProjectMember> findByProjectIdOrderByJoinedOnAscIdAsc(Long projectId);
}