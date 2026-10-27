package com.company.pms.service;

import com.company.pms.dto.response.ProjectProgressResponse;

public interface ReportService {

    ProjectProgressResponse getProjectProgress(Long projectId);
}