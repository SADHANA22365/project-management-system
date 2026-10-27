package com.company.pms.controller;

import com.company.pms.dto.request.UpdateIssueAssigneeRequest;
import com.company.pms.dto.request.UpdateIssueStatusRequest;
import com.company.pms.dto.response.IssueResponse;
import com.company.pms.dto.response.IssueStatisticsResponse;
import com.company.pms.dto.response.PageResponse;
import com.company.pms.enums.IssueSeverity;
import com.company.pms.enums.IssueStatus;
import com.company.pms.enums.IssueType;
import com.company.pms.exception.BusinessRuleException;
import com.company.pms.service.IssueService;
import com.company.pms.support.MockMvcSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class IssueControllerTest {

    @Mock
    private IssueService issueService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcSupport.forControllers(new IssueController(issueService));
    }

    private static IssueResponse response() {
        return new IssueResponse(40L, "Login crash", null, IssueType.BUG, IssueSeverity.MINOR,
                IssueStatus.OPEN, null, 10L, null, null, 1L, "Ann Lee", null, null);
    }

    @Test
    void createReturns201() throws Exception {
        when(issueService.create(eq(10L), any())).thenReturn(response());

        mockMvc.perform(post("/api/v1/projects/10/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Login crash\",\"type\":\"BUG\",\"reporterId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(40))
                .andExpect(jsonPath("$.type").value("BUG"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void createRejectsAMissingTypeWith400() throws Exception {
        mockMvc.perform(post("/api/v1/projects/10/issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Login crash\",\"reporterId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.type").value("Type is required"));

        verifyNoInteractions(issueService);
    }

    @Test
    void listPassesTheFiltersAndReturnsAPage() throws Exception {
        when(issueService.getByProject(eq(10L), eq(IssueStatus.OPEN), eq(IssueType.BUG),
                isNull(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(response()), 0, 10, 1, 1));

        mockMvc.perform(get("/api/v1/projects/10/issues")
                        .param("status", "OPEN")
                        .param("type", "BUG"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Login crash"));
    }

    @Test
    void listUsesPageSize10AndSortsByIdByDefault() throws Exception {
        when(issueService.getByProject(eq(10L), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0));

        mockMvc.perform(get("/api/v1/projects/10/issues")).andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(issueService).getByProject(eq(10L), isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(10);
        assertThat(captor.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void listRejectsAnUnknownStatusValueWith400() throws Exception {
        mockMvc.perform(get("/api/v1/projects/10/issues").param("status", "XYZ"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'XYZ' for parameter 'status'"));

        verifyNoInteractions(issueService);
    }

    @Test
    void statusChangePassesTheNewStatusToTheService() throws Exception {
        when(issueService.updateStatus(eq(40L), any(UpdateIssueStatusRequest.class)))
                .thenReturn(response());

        mockMvc.perform(patch("/api/v1/issues/40/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<UpdateIssueStatusRequest> captor =
                ArgumentCaptor.forClass(UpdateIssueStatusRequest.class);
        verify(issueService).updateStatus(eq(40L), captor.capture());
        assertThat(captor.getValue().status()).isEqualTo(IssueStatus.IN_PROGRESS);
    }

    @Test
    void statusChangeRejectsAnUnknownStatusNameWith400() throws Exception {
        mockMvc.perform(patch("/api/v1/issues/40/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOPE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));

        verifyNoInteractions(issueService);
    }

    @Test
    void anIllegalStatusMoveReturns422WithTheAllowedMoves() throws Exception {
        when(issueService.updateStatus(eq(40L), any(UpdateIssueStatusRequest.class)))
                .thenThrow(new BusinessRuleException(
                        "Cannot change issue status from OPEN to RESOLVED. Allowed: [IN_PROGRESS, CLOSED]"));

        mockMvc.perform(patch("/api/v1/issues/40/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message")
                        .value("Cannot change issue status from OPEN to RESOLVED. Allowed: [IN_PROGRESS, CLOSED]"));
    }

    @Test
    void aNullAssigneeIsAcceptedAndMeansUnassign() throws Exception {
        when(issueService.updateAssignee(eq(40L), any(UpdateIssueAssigneeRequest.class)))
                .thenReturn(response());

        mockMvc.perform(patch("/api/v1/issues/40/assignee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":null}"))
                .andExpect(status().isOk());

        ArgumentCaptor<UpdateIssueAssigneeRequest> captor =
                ArgumentCaptor.forClass(UpdateIssueAssigneeRequest.class);
        verify(issueService).updateAssignee(eq(40L), captor.capture());
        assertThat(captor.getValue().assigneeId()).isNull();
    }

    @Test
    void statisticsReturnsTheTotals() throws Exception {
        when(issueService.getStatistics(10L)).thenReturn(new IssueStatisticsResponse(
                10L, 3, 2, 1, Map.of(), Map.of(), Map.of(), List.of()));

        mockMvc.perform(get("/api/v1/projects/10/issues/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(10))
                .andExpect(jsonPath("$.totalIssues").value(3))
                .andExpect(jsonPath("$.openBugs").value(1));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/issues/40")).andExpect(status().isNoContent());

        verify(issueService).delete(40L);
    }
}