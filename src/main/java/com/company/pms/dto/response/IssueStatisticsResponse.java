package com.company.pms.dto.response;

import com.company.pms.enums.IssueSeverity;
import com.company.pms.enums.IssueStatus;
import com.company.pms.enums.IssueType;

import java.util.List;
import java.util.Map;

public record IssueStatisticsResponse(
        Long projectId,
        long totalIssues,
        long openIssues,
        long openBugs,
        Map<IssueStatus, Long> byStatus,
        Map<IssueType, Long> byType,
        Map<IssueSeverity, Long> bySeverity,
        List<AssigneeBugCount> openBugsByAssignee
) {

    public record AssigneeBugCount(
            Long assigneeId,
            String assigneeName,
            long openBugs
    ) {
    }
}