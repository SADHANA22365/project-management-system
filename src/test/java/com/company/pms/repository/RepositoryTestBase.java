package com.company.pms.repository;

import com.company.pms.entity.Issue;
import com.company.pms.entity.Project;
import com.company.pms.entity.Task;
import com.company.pms.entity.User;
import com.company.pms.enums.Designation;
import com.company.pms.enums.IssueSeverity;
import com.company.pms.enums.IssueStatus;
import com.company.pms.enums.IssueType;
import com.company.pms.enums.TaskStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
abstract class RepositoryTestBase {

    protected static final LocalDate TODAY = LocalDate.of(2026, 6, 15);

    @PersistenceContext
    protected EntityManager entityManager;

    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected ProjectRepository projectRepository;
    @Autowired
    protected TaskRepository taskRepository;
    @Autowired
    protected IssueRepository issueRepository;
    @Autowired
    protected CommentRepository commentRepository;

    protected User saveUser(String fullName) {
        String email = fullName.toLowerCase().replace(' ', '.') + "@example.com";
        return userRepository.save(new User(fullName, email, Designation.DEVELOPER));
    }

    protected Project saveProject(String name, User manager) {
        return projectRepository.save(
                new Project(name, null, LocalDate.of(2026, 1, 1), null, manager));
    }

    protected Task saveTask(Project project, User reporter, User assignee,
                            TaskStatus status, LocalDate dueDate) {
        Task task = new Task("Task", null, null, dueDate, null, project, assignee, reporter);
        task.setStatus(status);
        return taskRepository.save(task);
    }

    protected Issue saveIssue(Project project, User reporter, User assignee,
                              IssueType type, IssueSeverity severity, IssueStatus status) {
        Issue issue = new Issue("Issue", null, type, severity, project, reporter, assignee);
        issue.changeStatus(status, LocalDateTime.now());
        return issueRepository.save(issue);
    }

    protected void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}