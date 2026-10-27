package com.company.pms.repository;

import com.company.pms.entity.Project;
import com.company.pms.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    @Override
    @EntityGraph(attributePaths = "manager")
    Page<Project> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "manager")
    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);

    @Query("select p.status as status, count(p) as total from Project p group by p.status")
    List<ProjectStatusCount> countGroupedByStatus();
}