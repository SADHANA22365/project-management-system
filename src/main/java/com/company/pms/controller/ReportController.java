package com.company.pms.controller;

import com.company.pms.dto.response.ProjectProgressResponse;
import com.company.pms.service.ReportService;
import com.company.pms.dto.response.DashboardResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.company.pms.dto.response.ProjectSummaryResponse;
import com.company.pms.dto.response.UserWorkloadResponse;

@RestController
@RequestMapping("/api/v1")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/projects/{projectId}/progress")
    public ProjectProgressResponse getProjectProgress(@PathVariable Long projectId) {
        return reportService.getProjectProgress(projectId);
    }

    @GetMapping("/dashboard")
    public DashboardResponse getDashboard() {
        return reportService.getDashboard();
    }

    @GetMapping("/projects/{projectId}/summary")
    public ProjectSummaryResponse getProjectSummary(@PathVariable Long projectId) {
        return reportService.getProjectSummary(projectId);
    }

    @GetMapping("/users/{userId}/workload")
    public UserWorkloadResponse getUserWorkload(@PathVariable Long userId) {
        return reportService.getUserWorkload(userId);
    }
}