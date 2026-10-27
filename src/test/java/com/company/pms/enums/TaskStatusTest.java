package com.company.pms.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskStatusTest {

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "TODO, IN_PROGRESS",
            "TODO, BLOCKED",
            "IN_PROGRESS, IN_REVIEW",
            "IN_PROGRESS, BLOCKED",
            "IN_PROGRESS, TODO",
            "IN_REVIEW, DONE",
            "IN_REVIEW, IN_PROGRESS",
            "BLOCKED, TODO",
            "BLOCKED, IN_PROGRESS",
            "DONE, IN_PROGRESS"
    })
    void allowedMovesAreAccepted(TaskStatus from, TaskStatus to) {
        assertTrue(from.canTransitionTo(to));
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
            "TODO, DONE",
            "TODO, IN_REVIEW",
            "IN_REVIEW, TODO",
            "IN_REVIEW, BLOCKED",
            "BLOCKED, DONE",
            "BLOCKED, IN_REVIEW",
            "DONE, TODO",
            "DONE, IN_REVIEW",
            "DONE, BLOCKED"
    })
    void forbiddenMovesAreRejected(TaskStatus from, TaskStatus to) {
        assertFalse(from.canTransitionTo(to));
    }

    @Test
    void noStatusCanMoveToItself() {
        for (TaskStatus status : TaskStatus.values()) {
            assertFalse(status.canTransitionTo(status), status + " must not move to itself");
        }
    }

    @Test
    void onlyDoneIsNotOpen() {
        for (TaskStatus status : TaskStatus.values()) {
            assertTrue(status.isOpen() == (status != TaskStatus.DONE), status.name());
        }
    }
}