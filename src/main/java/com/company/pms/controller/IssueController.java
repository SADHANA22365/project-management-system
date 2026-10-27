package com.company.pms.controller;

import com.company.pms.dto.request.CreateIssueRequest;
import com.company.pms.dto.request.UpdateIssueAssigneeRequest;
import com.company.pms.dto.request.UpdateIssueRequest;
import com.company.pms.dto.request.UpdateIssueStatusRequest;
import com.company.pms.dto.response.IssueResponse;
import com.company.pms.dto.response.PageResponse;
import com.company.pms.enums.IssueSeverity;
import com.company.pms.enums.IssueStatus;
import com.company.pms.enums.IssueType;
import com.company.pms.service.IssueService;
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
public class IssueController {

    private final IssueService issueService;

    public IssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @PostMapping("/projects/{projectId}/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public IssueResponse create(@PathVariable Long projectId,
                                @Valid @RequestBody CreateIssueRequest request) {
        return issueService.create(projectId, request);
    }

    @GetMapping("/projects/{projectId}/issues")
    public PageResponse<IssueResponse> getByProject(
            @PathVariable Long projectId,
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(required = false) IssueType type,
            @RequestParam(required = false) IssueSeverity severity,
            @PageableDefault(sort = "id") Pageable pageable) {
        return issueService.getByProject(projectId, status, type, severity, pageable);
    }

    @GetMapping("/issues/{id}")
    public IssueResponse getById(@PathVariable Long id) {
        return issueService.getById(id);
    }

    @PutMapping("/issues/{id}")
    public IssueResponse update(@PathVariable Long id,
                                @Valid @RequestBody UpdateIssueRequest request) {
        return issueService.update(id, request);
    }

    @PatchMapping("/issues/{id}/status")
    public IssueResponse updateStatus(@PathVariable Long id,
                                      @Valid @RequestBody UpdateIssueStatusRequest request) {
        return issueService.updateStatus(id, request);
    }

    @PatchMapping("/issues/{id}/assignee")
    public IssueResponse updateAssignee(@PathVariable Long id,
                                        @RequestBody UpdateIssueAssigneeRequest request) {
        return issueService.updateAssignee(id, request);
    }

    @DeleteMapping("/issues/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        issueService.delete(id);
    }
}