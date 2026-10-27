package com.company.pms.enums;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum IssueStatus {

    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    REOPENED;

    private static final Map<IssueStatus, Set<IssueStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(IssueStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(OPEN, EnumSet.of(IN_PROGRESS, CLOSED));
        ALLOWED_TRANSITIONS.put(IN_PROGRESS, EnumSet.of(RESOLVED, OPEN));
        ALLOWED_TRANSITIONS.put(RESOLVED, EnumSet.of(CLOSED, REOPENED));
        ALLOWED_TRANSITIONS.put(CLOSED, EnumSet.of(REOPENED));
        ALLOWED_TRANSITIONS.put(REOPENED, EnumSet.of(IN_PROGRESS, CLOSED));
    }

    public boolean canTransitionTo(IssueStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    public Set<IssueStatus> allowedTransitions() {
        return Collections.unmodifiableSet(ALLOWED_TRANSITIONS.get(this));
    }

    public boolean isOpen() {
        return this == OPEN || this == IN_PROGRESS || this == REOPENED;
    }
}