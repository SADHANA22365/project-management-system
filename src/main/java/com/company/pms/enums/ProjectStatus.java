package com.company.pms.enums;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public enum ProjectStatus {

    PLANNED,
    ACTIVE,
    ON_HOLD,
    COMPLETED,
    CANCELLED;

    private static final Map<ProjectStatus, Set<ProjectStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(ProjectStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(PLANNED, EnumSet.of(ACTIVE, CANCELLED));
        ALLOWED_TRANSITIONS.put(ACTIVE, EnumSet.of(ON_HOLD, COMPLETED, CANCELLED));
        ALLOWED_TRANSITIONS.put(ON_HOLD, EnumSet.of(ACTIVE, CANCELLED));
        ALLOWED_TRANSITIONS.put(COMPLETED, EnumSet.noneOf(ProjectStatus.class));
        ALLOWED_TRANSITIONS.put(CANCELLED, EnumSet.noneOf(ProjectStatus.class));
    }

    public boolean canTransitionTo(ProjectStatus target) {
        return ALLOWED_TRANSITIONS.get(this).contains(target);
    }

    public Set<ProjectStatus> allowedTransitions() {
        return Collections.unmodifiableSet(ALLOWED_TRANSITIONS.get(this));
    }

    public boolean isClosed() {
        return this == COMPLETED || this == CANCELLED;
    }
}