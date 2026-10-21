package com.company.pms.entity;

import com.company.pms.enums.ProjectRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "project_members",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_project_member",
                columnNames = {"project_id", "user_id"}))
public class ProjectMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProjectRole projectRole;

    @Column(nullable = false)
    private LocalDate joinedOn;

    protected ProjectMember() {
    }

    public ProjectMember(Project project, User user, ProjectRole projectRole) {
        this.project = project;
        this.user = user;
        this.projectRole = projectRole;
        this.joinedOn = LocalDate.now();
    }

    public Project getProject() {
        return project;
    }

    public User getUser() {
        return user;
    }

    public ProjectRole getProjectRole() {
        return projectRole;
    }

    public void setProjectRole(ProjectRole projectRole) {
        this.projectRole = projectRole;
    }

    public LocalDate getJoinedOn() {
        return joinedOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProjectMember other)) {
            return false;
        }
        return getId() != null && Objects.equals(getId(), other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}