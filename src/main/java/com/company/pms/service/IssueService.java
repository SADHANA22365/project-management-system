package com.company.pms.service;

import com.company.pms.dto.request.CreateIssueRequest;
import com.company.pms.dto.request.UpdateIssueAssigneeRequest;
import com.company.pms.dto.request.UpdateIssueRequest;
import com.company.pms.dto.request.UpdateIssueStatusRequest;
import com.company.pms.dto.response.IssueResponse;
import com.company.pms.dto.response.PageResponse;
import com.company.pms.enums.IssueSeverity;
import com.company.pms.enums.IssueStatus;
import com.company.pms.enums.IssueType;
import org.springframework.data.domain.Pageable;

public interface IssueService {

    IssueResponse create(Long projectId, CreateIssueRequest request);

    PageResponse<IssueResponse> getByProject(Long projectId, IssueStatus status,
                                             IssueType type, IssueSeverity severity,
                                             Pageable pageable);

    IssueResponse getById(Long id);

    IssueResponse update(Long id, UpdateIssueRequest request);

    IssueResponse updateStatus(Long id, UpdateIssueStatusRequest request);

    IssueResponse updateAssignee(Long id, UpdateIssueAssigneeRequest request);

    void delete(Long id);
}