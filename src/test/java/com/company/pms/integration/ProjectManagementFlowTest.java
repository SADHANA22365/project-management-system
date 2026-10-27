package com.company.pms.integration;

import com.company.pms.repository.CommentRepository;
import com.company.pms.repository.IssueRepository;
import com.company.pms.repository.ProjectMemberRepository;
import com.company.pms.repository.ProjectRepository;
import com.company.pms.repository.TaskRepository;
import com.company.pms.repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class ProjectManagementFlowTest {

    @Autowired
    private WebApplicationContext context;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectMemberRepository memberRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private IssueRepository issueRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    private void cleanDatabase() {
        commentRepository.deleteAll();
        issueRepository.deleteAll();
        taskRepository.deleteAll();
        memberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---------- helpers ----------

    private MvcResult send(MockHttpServletRequestBuilder request, String json, int expectedStatus)
            throws Exception {
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(request).andExpect(status().is(expectedStatus)).andReturn();
    }

    private static long idOf(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private long createUser(String name, String email) throws Exception {
        String body = """
                {"fullName":"%s","email":"%s","designation":"DEVELOPER"}""".formatted(name, email);
        return idOf(send(post("/api/v1/users"), body, 201));
    }

    private long createProject(String name, long managerId) throws Exception {
        String body = """
                {"name":"%s","startDate":"2026-01-01","managerId":%d}""".formatted(name, managerId);
        return idOf(send(post("/api/v1/projects"), body, 201));
    }

    private void addMember(long projectId, long userId, String role) throws Exception {
        String body = """
                {"userId":%d,"projectRole":"%s"}""".formatted(userId, role);
        send(post("/api/v1/projects/" + projectId + "/members"), body, 201);
    }

    private long createTask(long projectId, long reporterId, long assigneeId) throws Exception {
        String body = """
                {"title":"Build login","reporterId":%d,"assigneeId":%d}""".formatted(reporterId, assigneeId);
        return idOf(send(post("/api/v1/projects/" + projectId + "/tasks"), body, 201));
    }

    private void setTaskStatus(long taskId, String status) throws Exception {
        send(patch("/api/v1/tasks/" + taskId + "/status"),
                "{\"status\":\"" + status + "\"}", 200);
    }

    private void setProjectStatus(long projectId, String status, int expectedStatus) throws Exception {
        send(patch("/api/v1/projects/" + projectId + "/status"),
                "{\"status\":\"" + status + "\"}", expectedStatus);
    }

    private long createIssue(long projectId, String type, String severity, long reporterId)
            throws Exception {
        String body = """
                {"title":"Login crash","type":"%s","severity":"%s","reporterId":%d}"""
                .formatted(type, severity, reporterId);
        return idOf(send(post("/api/v1/projects/" + projectId + "/issues"), body, 201));
    }

    private void createComment(long taskId, long authorId, String content) throws Exception {
        String body = """
                {"authorId":%d,"content":"%s"}""".formatted(authorId, content);
        send(post("/api/v1/tasks/" + taskId + "/comments"), body, 201);
    }

    // ---------- tests ----------

    @Test
    void fullLifecycleFromCreatingAUserToDeletingTheProject() throws Exception {
        long ann = createUser("Ann Lee", "ann@example.com");
        long bob = createUser("Bob Ray", "bob@example.com");
        long project = createProject("Alpha", ann);
        addMember(project, bob, "MEMBER");
        long task = createTask(project, ann, bob);

        createComment(task, bob, "Looks good");
        mockMvc.perform(get("/api/v1/tasks/" + task + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].authorName").value("Bob Ray"));

        long issue = createIssue(project, "BUG", "MAJOR", bob);
        send(patch("/api/v1/issues/" + issue + "/status"), "{\"status\":\"IN_PROGRESS\"}", 200);
        mockMvc.perform(patch("/api/v1/issues/" + issue + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

        mockMvc.perform(get("/api/v1/projects/" + project + "/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(1))
                .andExpect(jsonPath("$.progressPercent").value(0.0));

        setTaskStatus(task, "IN_PROGRESS");
        setTaskStatus(task, "IN_REVIEW");
        setTaskStatus(task, "DONE");

        mockMvc.perform(get("/api/v1/projects/" + project + "/progress"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doneTasks").value(1))
                .andExpect(jsonPath("$.progressPercent").value(100.0));

        mockMvc.perform(get("/api/v1/projects/" + project + "/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.managerName").value("Ann Lee"))
                .andExpect(jsonPath("$.memberCount").value(2))
                .andExpect(jsonPath("$.progress.doneTasks").value(1));

        mockMvc.perform(get("/api/v1/users/" + bob + "/workload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openTasks").value(0));

        mockMvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProjects").value(1))
                .andExpect(jsonPath("$.totalTasks").value(1))
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.overdueTasks").value(0));

        setProjectStatus(project, "ACTIVE", 200);
        setProjectStatus(project, "COMPLETED", 200);

        // a completed project is read-only for comments and issues, but still readable
        send(post("/api/v1/tasks/" + task + "/comments"),
                "{\"authorId\":" + bob + ",\"content\":\"too late\"}", 422);
        send(post("/api/v1/projects/" + project + "/issues"),
                "{\"title\":\"Late\",\"type\":\"BUG\",\"reporterId\":" + bob + "}", 422);
        mockMvc.perform(get("/api/v1/tasks/" + task + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        send(delete("/api/v1/projects/" + project), null, 204);

        send(get("/api/v1/projects/" + project), null, 404);
        send(get("/api/v1/tasks/" + task), null, 404);
        send(get("/api/v1/issues/" + issue), null, 404);
        assertThat(commentRepository.count()).isZero();
        assertThat(issueRepository.count()).isZero();
        assertThat(taskRepository.count()).isZero();
        assertThat(memberRepository.count()).isZero();
        assertThat(userRepository.count()).isEqualTo(2);
    }

    @Test
    void deletingAProjectWithLiveDataRemovesEverythingInsideIt() throws Exception {
        long ann = createUser("Ann Lee", "ann@example.com");
        long project = createProject("Alpha", ann);
        long task = createTask(project, ann, ann);
        createComment(task, ann, "Note");
        createIssue(project, "BUG", "MINOR", ann);

        send(delete("/api/v1/projects/" + project), null, 204);

        assertThat(commentRepository.count()).isZero();
        assertThat(issueRepository.count()).isZero();
        assertThat(taskRepository.count()).isZero();
        assertThat(memberRepository.count()).isZero();
        assertThat(projectRepository.count()).isZero();
    }

    @Test
    void aProjectCannotBeCompletedWhileATaskIsNotDone() throws Exception {
        long ann = createUser("Ann Lee", "ann@example.com");
        long project = createProject("Alpha", ann);
        createTask(project, ann, ann);
        setProjectStatus(project, "ACTIVE", 200);

        mockMvc.perform(patch("/api/v1/projects/" + project + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message")
                        .value("Project cannot be completed while it has tasks that are not DONE"));
    }

    @Test
    void aMemberWithOpenTasksCanOnlyBeRemovedOnceTheyAreDone() throws Exception {
        long ann = createUser("Ann Lee", "ann@example.com");
        long bob = createUser("Bob Ray", "bob@example.com");
        long project = createProject("Alpha", ann);
        addMember(project, bob, "MEMBER");
        long task = createTask(project, ann, bob);

        mockMvc.perform(delete("/api/v1/projects/" + project + "/members/" + bob))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message")
                        .value("Cannot remove a member who still has open tasks in this project"));

        setTaskStatus(task, "IN_PROGRESS");
        setTaskStatus(task, "IN_REVIEW");
        setTaskStatus(task, "DONE");

        send(delete("/api/v1/projects/" + project + "/members/" + bob), null, 204);
    }

    @Test
    void theOnlyLeadCannotBeDemoted() throws Exception {
        long ann = createUser("Ann Lee", "ann@example.com");
        long project = createProject("Alpha", ann);

        mockMvc.perform(patch("/api/v1/projects/" + project + "/members/" + ann)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectRole\":\"MEMBER\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("A project must always have at least one LEAD"));
    }

    @Test
    void aDuplicateEmailIsRejectedEvenWhenTheCaseDiffers() throws Exception {
        createUser("Ann Lee", "ann@example.com");

        send(post("/api/v1/users"),
                "{\"fullName\":\"Another Ann\",\"email\":\"ANN@EXAMPLE.COM\",\"designation\":\"TESTER\"}",
                409);
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void anIllegalIssueStatusMoveIsRejectedWithTheAllowedMoves() throws Exception {
        long ann = createUser("Ann Lee", "ann@example.com");
        long project = createProject("Alpha", ann);
        long issue = createIssue(project, "BUG", "MAJOR", ann);

        mockMvc.perform(patch("/api/v1/issues/" + issue + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value(
                        "Cannot change issue status from OPEN to RESOLVED. Allowed: [IN_PROGRESS, CLOSED]"));
    }

    @Test
    void issueFiltersAndStatisticsWorkOverHttp() throws Exception {
        long ann = createUser("Ann Lee", "ann@example.com");
        long project = createProject("Alpha", ann);
        createIssue(project, "BUG", "MAJOR", ann);
        createIssue(project, "IMPROVEMENT", "MINOR", ann);

        mockMvc.perform(get("/api/v1/projects/" + project + "/issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/projects/" + project + "/issues").param("type", "BUG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/v1/projects/" + project + "/issues/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIssues").value(2))
                .andExpect(jsonPath("$.openBugs").value(1))
                .andExpect(jsonPath("$.byType.BUG").value(1))
                .andExpect(jsonPath("$.byType.SUPPORT").value(0));
    }

    @Test
    void reportsForUnknownIdsReturn404() throws Exception {
        send(get("/api/v1/projects/999999/progress"), null, 404);
        send(get("/api/v1/projects/999999/summary"), null, 404);
        send(get("/api/v1/projects/999999/issues/statistics"), null, 404);
        send(get("/api/v1/users/999999/workload"), null, 404);
    }
}