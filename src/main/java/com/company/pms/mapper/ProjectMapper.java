package com.company.pms.mapper;

import com.company.pms.dto.request.CreateProjectRequest;
import com.company.pms.dto.request.UpdateProjectRequest;
import com.company.pms.dto.response.ProjectResponse;
import com.company.pms.entity.Project;
import com.company.pms.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public Project toEntity(CreateProjectRequest request, User manager) {
        return new Project(
                request.name().trim(),
                request.description(),
                request.startDate(),
                request.endDate(),
                manager);
    }

    public void updateEntity(Project project, UpdateProjectRequest request) {
        project.setName(request.name().trim());
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
    }

    public ProjectResponse toResponse(Project project) {
        User manager = project.getManager();
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStartDate(),
                project.getEndDate(),
                project.getStatus(),
                manager.getId(),
                manager.getFullName(),
                project.getCreatedAt(),
                project.getUpdatedAt());
    }
}