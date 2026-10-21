package com.company.pms.service;

import com.company.pms.dto.request.CreateProjectRequest;
import com.company.pms.dto.request.UpdateProjectRequest;
import com.company.pms.dto.request.UpdateProjectStatusRequest;
import com.company.pms.dto.response.PageResponse;
import com.company.pms.dto.response.ProjectResponse;
import com.company.pms.enums.ProjectStatus;
import org.springframework.data.domain.Pageable;

public interface ProjectService {

    ProjectResponse create(CreateProjectRequest request);

    ProjectResponse getById(Long id);

    PageResponse<ProjectResponse> getAll(ProjectStatus status, Pageable pageable);

    ProjectResponse update(Long id, UpdateProjectRequest request);

    ProjectResponse updateStatus(Long id, UpdateProjectStatusRequest request);

    void delete(Long id);
}