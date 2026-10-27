package com.company.pms.service;

import com.company.pms.dto.response.ProjectProgressResponse;
import com.company.pms.dto.response.DashboardResponse;

public interface ReportService {

    ProjectProgressResponse getProjectProgress(Long projectId);

    DashboardResponse getDashboard();
}