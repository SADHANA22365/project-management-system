package com.company.pms.entity;

import com.company.pms.enums.IssueSeverity;
import com.company.pms.enums.IssueStatus;
import com.company.pms.enums.IssueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class IssueTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 10, 28, 10, 0);

    private Issue issue;

    @BeforeEach
    void setUp() {
        issue = new Issue("Login crash", "App crashes", IssueType.BUG,
                null, null, null, null);
    }

    @Test
    void newIssueStartsOpenWithDefaultSeverityAndNoResolvedDate() {
        assertEquals(IssueStatus.OPEN, issue.getStatus());
        assertEquals(IssueSeverity.MINOR, issue.getSeverity());
        assertNull(issue.getResolvedAt());
    }

    @Test
    void givenSeverityIsKept() {
        Issue critical = new Issue("Data loss", null, IssueType.BUG,
                IssueSeverity.CRITICAL, null, null, null);
        assertEquals(IssueSeverity.CRITICAL, critical.getSeverity());
    }

    @Test
    void resolvingSetsResolvedAt() {
        issue.changeStatus(IssueStatus.RESOLVED, now);

        assertEquals(IssueStatus.RESOLVED, issue.getStatus());
        assertEquals(now, issue.getResolvedAt());
    }

    @Test
    void closingAfterResolvingKeepsResolvedAt() {
        issue.changeStatus(IssueStatus.RESOLVED, now);
        issue.changeStatus(IssueStatus.CLOSED, now.plusDays(1));

        assertEquals(IssueStatus.CLOSED, issue.getStatus());
        assertEquals(now, issue.getResolvedAt());
    }

    @Test
    void closingWithoutResolvingLeavesResolvedAtEmpty() {
        issue.changeStatus(IssueStatus.CLOSED, now);

        assertEquals(IssueStatus.CLOSED, issue.getStatus());
        assertNull(issue.getResolvedAt());
    }

    @Test
    void reopeningClearsResolvedAt() {
        issue.changeStatus(IssueStatus.RESOLVED, now);
        assertNotNull(issue.getResolvedAt());

        issue.changeStatus(IssueStatus.REOPENED, now.plusDays(2));

        assertEquals(IssueStatus.REOPENED, issue.getStatus());
        assertNull(issue.getResolvedAt());
    }
}