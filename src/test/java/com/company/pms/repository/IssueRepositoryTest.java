package com.company.pms.repository;

import com.company.pms.entity.Project;
import com.company.pms.entity.User;
import com.company.pms.enums.IssueSeverity;
import com.company.pms.enums.IssueStatus;
import com.company.pms.enums.IssueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class IssueRepositoryTest extends RepositoryTestBase {

    private final Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));

    private User ann;
    private User bob;
    private Project alpha;
    private Project beta;

    @BeforeEach
    void setUp() {
        ann = saveUser("Ann Lee");
        bob = saveUser("Bob Ray");
        alpha = saveProject("Alpha", ann);
        beta = saveProject("Beta", ann);
    }

    // ---------- search (optional filters) ----------

    @Test
    void searchWithNoFiltersReturnsEveryIssueOfTheProjectOnly() {
        saveIssue(alpha, ann, null, IssueType.BUG, IssueSeverity.MAJOR, IssueStatus.OPEN);
        saveIssue(alpha, ann, null, IssueType.BUG, IssueSeverity.MINOR, IssueStatus.IN_PROGRESS);
        saveIssue(alpha, ann, null, IssueType.IMPROVEMENT, IssueSeverity.MINOR, IssueStatus.OPEN);
        saveIssue(beta, ann, null, IssueType.BUG, IssueSeverity.MAJOR, IssueStatus.OPEN);

        Page<?> result = issueRepository.search(alpha.getId(), null, null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void searchAppliesOnlyTheFiltersThatAreGiven() {
        saveIssue(alpha, ann, null, IssueType.BUG, IssueSeverity.MAJOR, IssueStatus.OPEN);
        saveIssue(alpha, ann, null, IssueType.BUG, IssueSeverity.MINOR, IssueStatus.IN_PROGRESS);
        saveIssue(alpha, ann, null, IssueType.IMPROVEMENT, IssueSeverity.MINOR, IssueStatus.OPEN);
        Long id = alpha.getId();

        assertThat(issueRepository.search(id, null, IssueType.BUG, null, pageable)
                .getTotalElements()).isEqualTo(2);
        assertThat(issueRepository.search(id, IssueStatus.OPEN, null, null, pageable)
                .getTotalElements()).isEqualTo(2);
        assertThat(issueRepository.search(id, IssueStatus.OPEN, IssueType.BUG, null, pageable)
                .getTotalElements()).isEqualTo(1);
        assertThat(issueRepository.search(id, null, null, IssueSeverity.MINOR, pageable)
                .getTotalElements()).isEqualTo(2);
        assertThat(issueRepository.search(id, IssueStatus.OPEN, IssueType.BUG, IssueSeverity.MINOR, pageable)
                .getTotalElements()).isZero();
    }

    @Test
    void searchPagesAndSortsTheResult() {
        for (int i = 0; i < 3; i++) {
            saveIssue(alpha, ann, null, IssueType.BUG, IssueSeverity.MINOR, IssueStatus.OPEN);
        }

        Page<?> firstPage = issueRepository.search(alpha.getId(), null, null, null,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "id")));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    // ---------- statistics queries ----------

    @Test
    void countGroupedByProjectGroupsByStatusTypeAndSeverity() {
        saveIssue(alpha, ann, null, IssueType.BUG, IssueSeverity.MAJOR, IssueStatus.OPEN);
        saveIssue(alpha, ann, null, IssueType.BUG, IssueSeverity.MAJOR, IssueStatus.OPEN);
        saveIssue(alpha, ann, null, IssueType.BUG, IssueSeverity.MINOR, IssueStatus.IN_PROGRESS);
        saveIssue(alpha, ann, null, IssueType.IMPROVEMENT, IssueSeverity.MINOR, IssueStatus.OPEN);
        saveIssue(beta, ann, null, IssueType.BUG, IssueSeverity.BLOCKER, IssueStatus.OPEN);

        List<IssueCount> rows = issueRepository.countGroupedByProject(alpha.getId());

        assertThat(rows)
                .extracting(IssueCount::getStatus, IssueCount::getType,
                        IssueCount::getSeverity, IssueCount::getTotal)
                .containsExactlyInAnyOrder(
                        tuple(IssueStatus.OPEN, IssueType.BUG, IssueSeverity.MAJOR, 2L),
                        tuple(IssueStatus.IN_PROGRESS, IssueType.BUG, IssueSeverity.MINOR, 1L),
                        tuple(IssueStatus.OPEN, IssueType.IMPROVEMENT, IssueSeverity.MINOR, 1L));
    }

    @Test
    void openBugsByAssigneeCountsOnlyAssignedOpenBugsOfThatProject() {
        saveIssue(alpha, ann, ann, IssueType.BUG, IssueSeverity.MAJOR, IssueStatus.OPEN);
        saveIssue(alpha, ann, ann, IssueType.BUG, IssueSeverity.MAJOR, IssueStatus.REOPENED);
        saveIssue(alpha, ann, bob, IssueType.BUG, IssueSeverity.MINOR, IssueStatus.IN_PROGRESS);
        saveIssue(alpha, ann, bob, IssueType.BUG, IssueSeverity.MINOR, IssueStatus.RESOLVED);
        saveIssue(alpha, ann, null, IssueType.BUG, IssueSeverity.MINOR, IssueStatus.OPEN);
        saveIssue(alpha, ann, ann, IssueType.IMPROVEMENT, IssueSeverity.MINOR, IssueStatus.OPEN);
        saveIssue(beta, ann, bob, IssueType.BUG, IssueSeverity.MINOR, IssueStatus.OPEN);

        List<OpenBugCount> rows = issueRepository.countOpenBugsByAssignee(alpha.getId(),
                List.of(IssueStatus.OPEN, IssueStatus.IN_PROGRESS, IssueStatus.REOPENED));

        assertThat(rows)
                .extracting(OpenBugCount::getAssigneeName, OpenBugCount::getTotal)
                .containsExactly(tuple("Ann Lee", 2L), tuple("Bob Ray", 1L));
    }

    // ---------- bulk delete ----------

    @Test
    void deleteByProjectIdRemovesOnlyThatProjectsIssues() {
        saveIssue(alpha, ann, null, IssueType.BUG, IssueSeverity.MINOR, IssueStatus.OPEN);
        saveIssue(alpha, ann, null, IssueType.BUG, IssueSeverity.MINOR, IssueStatus.OPEN);
        saveIssue(beta, ann, null, IssueType.BUG, IssueSeverity.MINOR, IssueStatus.OPEN);

        issueRepository.deleteByProjectId(alpha.getId());

        assertThat(issueRepository.search(alpha.getId(), null, null, null, pageable)
                .getTotalElements()).isZero();
        assertThat(issueRepository.search(beta.getId(), null, null, null, pageable)
                .getTotalElements()).isEqualTo(1);
    }
}