package com.company.pms.service;

import com.company.pms.dto.request.CreateTaskRequest;
import com.company.pms.dto.request.UpdateTaskAssigneeRequest;
import com.company.pms.dto.request.UpdateTaskRequest;
import com.company.pms.dto.request.UpdateTaskStatusRequest;
import com.company.pms.dto.response.PageResponse;
import com.company.pms.dto.response.TaskResponse;
import com.company.pms.enums.TaskStatus;
import org.springframework.data.domain.Pageable;

public interface TaskService {

    TaskResponse create(Long projectId, CreateTaskRequest request);

    PageResponse<TaskResponse> getByProject(Long projectId, TaskStatus status, Pageable pageable);

    TaskResponse getById(Long id);

    TaskResponse update(Long id, UpdateTaskRequest request);

    TaskResponse updateStatus(Long id, UpdateTaskStatusRequest request);

    TaskResponse updateAssignee(Long id, UpdateTaskAssigneeRequest request);

    void delete(Long id);

    PageResponse<TaskResponse> getOverdue(Pageable pageable);

    TaskResponse addLabel(Long taskId, Long labelId);

    TaskResponse removeLabel(Long taskId, Long labelId);
}