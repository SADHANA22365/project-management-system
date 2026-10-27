package com.company.pms.enums;

public enum IssueSeverity {

    MINOR,
    MAJOR,
    CRITICAL,
    BLOCKER;

    public static final IssueSeverity DEFAULT = MINOR;
}