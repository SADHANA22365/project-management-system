package com.company.pms.controller;

import com.company.pms.dto.request.CreateTaskRequest;
import com.company.pms.dto.request.UpdateTaskAssigneeRequest;
import com.company.pms.dto.request.UpdateTaskRequest;
import com.company.pms.dto.request.UpdateTaskStatusRequest;
import com.company.pms.dto.response.PageResponse;
import com.company.pms.dto.response.TaskResponse;
import com.company.pms.enums.TaskStatus;
import com.company.pms.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping("/projects/{projectId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(@PathVariable Long projectId,
                               @Valid @RequestBody CreateTaskRequest request) {
        return taskService.create(projectId, request);
    }

    @GetMapping("/projects/{projectId}/tasks")
    public PageResponse<TaskResponse> getByProject(
            @PathVariable Long projectId,
            @RequestParam(required = false) TaskStatus status,
            @PageableDefault(sort = "id") Pageable pageable) {
        return taskService.getByProject(projectId, status, pageable);
    }

    @GetMapping("/tasks/overdue")
    public PageResponse<TaskResponse> getOverdue(
            @PageableDefault(sort = "dueDate") Pageable pageable) {
        return taskService.getOverdue(pageable);
    }

    @GetMapping("/tasks/{id}")
    public TaskResponse getById(@PathVariable Long id) {
        return taskService.getById(id);
    }

    @PutMapping("/tasks/{id}")
    public TaskResponse update(@PathVariable Long id,
                               @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(id, request);
    }

    @PatchMapping("/tasks/{id}/status")
    public TaskResponse updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UpdateTaskStatusRequest request) {
        return taskService.updateStatus(id, request);
    }

    @PatchMapping("/tasks/{id}/assignee")
    public TaskResponse updateAssignee(@PathVariable Long id,
                                       @RequestBody UpdateTaskAssigneeRequest request) {
        return taskService.updateAssignee(id, request);
    }

    @PutMapping("/tasks/{taskId}/labels/{labelId}")
    public TaskResponse addLabel(@PathVariable Long taskId,
                                 @PathVariable Long labelId) {
        return taskService.addLabel(taskId, labelId);
    }

    @DeleteMapping("/tasks/{taskId}/labels/{labelId}")
    public TaskResponse removeLabel(@PathVariable Long taskId,
                                    @PathVariable Long labelId) {
        return taskService.removeLabel(taskId, labelId);
    }

    @DeleteMapping("/tasks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        taskService.delete(id);
    }
}