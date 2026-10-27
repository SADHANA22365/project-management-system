package com.company.pms.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IssueStatusTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "OPEN, IN_PROGRESS",
            "OPEN, CLOSED",
            "IN_PROGRESS, RESOLVED",
            "IN_PROGRESS, OPEN",
            "RESOLVED, CLOSED",
            "RESOLVED, REOPENED",
            "CLOSED, REOPENED",
            "REOPENED, IN_PROGRESS",
            "REOPENED, CLOSED"
    })
    void allowedMovesAreAccepted(IssueStatus from, IssueStatus to) {
        assertTrue(from.canTransitionTo(to));
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
            "OPEN, RESOLVED",
            "OPEN, REOPENED",
            "IN_PROGRESS, CLOSED",
            "IN_PROGRESS, REOPENED",
            "RESOLVED, OPEN",
            "RESOLVED, IN_PROGRESS",
            "CLOSED, OPEN",
            "CLOSED, IN_PROGRESS",
            "CLOSED, RESOLVED",
            "REOPENED, OPEN",
            "REOPENED, RESOLVED"
    })
    void forbiddenMovesAreRejected(IssueStatus from, IssueStatus to) {
        assertFalse(from.canTransitionTo(to));
    }

    @Test
    void noStatusCanMoveToItself() {
        for (IssueStatus status : IssueStatus.values()) {
            assertFalse(status.canTransitionTo(status), status + " must not move to itself");
        }
    }

    @ParameterizedTest(name = "{0}.isOpen() is {1}")
    @CsvSource({
            "OPEN, true",
            "IN_PROGRESS, true",
            "REOPENED, true",
            "RESOLVED, false",
            "CLOSED, false"
    })
    void openStatusesAreTheOnesStillBeingWorkedOn(IssueStatus status, boolean expected) {
        assertEquals(expected, status.isOpen());
    }

    @Test
    void closedCanOnlyBeReopened() {
        assertEquals(1, IssueStatus.CLOSED.allowedTransitions().size());
        assertTrue(IssueStatus.CLOSED.allowedTransitions().contains(IssueStatus.REOPENED));
    }
}