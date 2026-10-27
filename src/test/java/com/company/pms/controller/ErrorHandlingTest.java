package com.company.pms.controller;

import com.company.pms.dto.request.CreateUserRequest;
import com.company.pms.dto.request.UpdateTaskStatusRequest;
import com.company.pms.exception.BusinessRuleException;
import com.company.pms.exception.DuplicateResourceException;
import com.company.pms.exception.InvalidRequestException;
import com.company.pms.exception.ResourceNotFoundException;
import com.company.pms.service.TaskService;
import com.company.pms.service.UserService;
import com.company.pms.support.MockMvcSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ErrorHandlingTest {

    @Mock
    private TaskService taskService;
    @Mock
    private UserService userService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcSupport.forControllers(
                new TaskController(taskService), new UserController(userService));
    }

    @Test
    void aMissingResourceBecomes404() throws Exception {
        when(taskService.getById(99L))
                .thenThrow(new ResourceNotFoundException("Task not found with id 99"));

        mockMvc.perform(get("/api/v1/tasks/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Task not found with id 99"))
                .andExpect(jsonPath("$.path").value("/api/v1/tasks/99"));
    }

    @Test
    void aDuplicateBecomes409() throws Exception {
        when(userService.create(any(CreateUserRequest.class)))
                .thenThrow(new DuplicateResourceException("A user with email 'a@b.com' already exists"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Ann Lee\",\"email\":\"a@b.com\",\"designation\":\"DEVELOPER\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A user with email 'a@b.com' already exists"));
    }

    @Test
    void aBrokenBusinessRuleBecomes422() throws Exception {
        when(taskService.updateStatus(eq(20L), any(UpdateTaskStatusRequest.class)))
                .thenThrow(new BusinessRuleException("Cannot change task status from TODO to DONE"));

        mockMvc.perform(patch("/api/v1/tasks/20/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.message").value("Cannot change task status from TODO to DONE"));
    }

    @Test
    void anInvalidRequestBecomes400() throws Exception {
        when(taskService.getOverdue(any(Pageable.class)))
                .thenThrow(new InvalidRequestException("Invalid sort field 'x'"));

        mockMvc.perform(get("/api/v1/tasks/overdue"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid sort field 'x'"));
    }

    @Test
    void validationErrorsListEveryBadField() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"\",\"email\":\"not-an-email\",\"designation\":\"DEVELOPER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.fullName").value("Full name is required"))
                .andExpect(jsonPath("$.fieldErrors.email").value("Email must be a valid email address"));

        verifyNoInteractions(userService);
    }

    @Test
    void aNonNumericIdBecomes400() throws Exception {
        mockMvc.perform(get("/api/v1/tasks/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'id'"));

        verifyNoInteractions(taskService);
    }

    @Test
    void anUnexpectedErrorBecomes500WithoutLeakingDetails() throws Exception {
        when(taskService.getById(1L)).thenThrow(new IllegalStateException("db password is hunter2"));

        String body = mockMvc.perform(get("/api/v1/tasks/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("hunter2");
    }
}