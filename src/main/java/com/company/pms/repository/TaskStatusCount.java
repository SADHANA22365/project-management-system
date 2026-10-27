package com.company.pms.repository;

import com.company.pms.enums.TaskStatus;

public interface TaskStatusCount {

    TaskStatus getStatus();

    long getTotal();
}