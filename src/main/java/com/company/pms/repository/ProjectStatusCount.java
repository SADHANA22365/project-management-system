package com.company.pms.repository;

import com.company.pms.enums.ProjectStatus;

public interface ProjectStatusCount {

    ProjectStatus getStatus();

    long getTotal();
}