package com.company.pms.controller;

import com.company.pms.dto.request.CreateProjectRequest;
import com.company.pms.dto.request.UpdateProjectRequest;
import com.company.pms.dto.request.UpdateProjectStatusRequest;
import com.company.pms.dto.response.PageResponse;
import com.company.pms.dto.response.ProjectResponse;
import com.company.pms.enums.ProjectStatus;
import com.company.pms.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Projects", description = "Create projects, change their status and view progress")
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(request));
    }

    @GetMapping
    public PageResponse<ProjectResponse> getAll(
            @RequestParam(required = false) ProjectStatus status,
            Pageable pageable) {
        return projectService.getAll(status, pageable);
    }

    @GetMapping("/{id}")
    public ProjectResponse getById(@PathVariable Long id) {
        return projectService.getById(id);
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id,
                                  @Valid @RequestBody UpdateProjectRequest request) {
        return projectService.update(id, request);
    }

    @Operation(summary = "Change project status",
        description = "Must follow the allowed transitions (422 otherwise). "
                + "COMPLETED requires every task to be DONE.")

    @PatchMapping("/{id}/status")
    public ProjectResponse updateStatus(@PathVariable Long id,
                                        @Valid @RequestBody UpdateProjectStatusRequest request) {
        return projectService.updateStatus(id, request);
    }

    @Operation(summary = "Delete a project",
        description = "Also deletes its tasks, comments, issues and members.")

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }
}