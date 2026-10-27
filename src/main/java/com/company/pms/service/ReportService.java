package com.company.pms.service;

import com.company.pms.dto.response.ProjectProgressResponse;
import com.company.pms.dto.response.DashboardResponse;
import com.company.pms.dto.response.ProjectSummaryResponse;
import com.company.pms.dto.response.UserWorkloadResponse;
public interface ReportService {

    ProjectProgressResponse getProjectProgress(Long projectId);

    DashboardResponse getDashboard();

    ProjectSummaryResponse getProjectSummary(Long projectId);

    UserWorkloadResponse getUserWorkload(Long userId);
}