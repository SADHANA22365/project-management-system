package com.company.pms.enums;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum TaskStatus {

    TODO,
    IN_PROGRESS,
    IN_REVIEW,
    BLOCKED,
    DONE;

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(TaskStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(TODO, EnumSet.of(IN_PROGRESS, BLOCKED));
        ALLOWED_TRANSITIONS.put(IN_PROGRESS, EnumSet.of(IN_REVIEW, BLOCKED, TODO));
        ALLOWED_TRANSITIONS.put(IN_REVIEW, EnumSet.of(DONE, IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(BLOCKED, EnumSet.of(TODO, IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(DONE, EnumSet.of(IN_PROGRESS));
    }

    public boolean canTransitionTo(TaskStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    public Set<TaskStatus> allowedTransitions() {
        return Collections.unmodifiableSet(ALLOWED_TRANSITIONS.get(this));
    }

    public boolean isOpen() {
        return this != DONE;
    }
}