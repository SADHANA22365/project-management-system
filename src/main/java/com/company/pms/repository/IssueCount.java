package com.company.pms.repository;

import com.company.pms.enums.IssueSeverity;
import com.company.pms.enums.IssueStatus;
import com.company.pms.enums.IssueType;

public interface IssueCount {

    IssueStatus getStatus();

    IssueType getType();

    IssueSeverity getSeverity();

    long getTotal();
}