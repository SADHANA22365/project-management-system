package com.company.pms.support;

import com.company.pms.entity.Project;
import com.company.pms.entity.User;
import com.company.pms.enums.Designation;
import com.company.pms.enums.ProjectStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

public final class TestData {

    public static final LocalDate START = LocalDate.of(2026, 1, 1);

    private TestData() {
    }

    public static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    public static User user(Long id, String fullName) {
        String email = fullName.toLowerCase().replace(' ', '.') + "@example.com";
        return withId(new User(fullName, email, Designation.DEVELOPER), id);
    }

    public static Project project(Long id, ProjectStatus status, User manager) {
        Project project = new Project("Project " + id, null, START, null, manager);
        project.setStatus(status);
        return withId(project, id);
    }
}