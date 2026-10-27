package com.company.pms.controller;

import com.company.pms.dto.response.ProjectProgressResponse;
import com.company.pms.service.ReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}