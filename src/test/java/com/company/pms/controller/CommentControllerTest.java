package com.company.pms.controller;

import com.company.pms.dto.request.CreateCommentRequest;
import com.company.pms.dto.request.UpdateCommentRequest;
import com.company.pms.dto.response.CommentResponse;
import com.company.pms.exception.BusinessRuleException;
import com.company.pms.exception.ResourceNotFoundException;
import com.company.pms.service.CommentService;
import com.company.pms.support.MockMvcSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcSupport.forControllers(new CommentController(commentService));
    }

    private static CommentResponse response(long id, String content) {
        return new CommentResponse(id, 20L, 1L, "Ann Lee", content, null, null);
    }

    @Test
    void createReturns201AndPassesTheRequestToTheService() throws Exception {
        when(commentService.create(eq(20L), any(CreateCommentRequest.class)))
                .thenReturn(response(30L, "Hello"));

        mockMvc.perform(post("/api/v1/tasks/20/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":1,\"content\":\"Hello\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.authorName").value("Ann Lee"))
                .andExpect(jsonPath("$.content").value("Hello"));

        ArgumentCaptor<CreateCommentRequest> captor = ArgumentCaptor.forClass(CreateCommentRequest.class);
        verify(commentService).create(eq(20L), captor.capture());
        assertThat(captor.getValue().authorId()).isEqualTo(1L);
        assertThat(captor.getValue().content()).isEqualTo("Hello");
    }

    @Test
    void createRejectsBlankContentWith400AndAFieldError() throws Exception {
        mockMvc.perform(post("/api/v1/tasks/20/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":1,\"content\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.content").value("Comment content is required"));

        verifyNoInteractions(commentService);
    }

    @Test
    void createRejectsAMissingAuthorId() throws Exception {
        mockMvc.perform(post("/api/v1/tasks/20/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Hello\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.authorId").value("Author id is required"));

        verifyNoInteractions(commentService);
    }

    @Test
    void createRejectsContentLongerThan2000Characters() throws Exception {
        String tooLong = "a".repeat(2001);

        mockMvc.perform(post("/api/v1/tasks/20/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":1,\"content\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.content")
                        .value("Comment must not exceed 2000 characters"));

        verifyNoInteractions(commentService);
    }

    @Test
    void createRejectsMalformedJsonWith400() throws Exception {
        mockMvc.perform(post("/api/v1/tasks/20/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed or unreadable request body"));

        verifyNoInteractions(commentService);
    }

    @Test
    void createReturns422WhenABusinessRuleFails() throws Exception {
        when(commentService.create(eq(20L), any(CreateCommentRequest.class)))
                .thenThrow(new BusinessRuleException("Author is not a member of this project"));

        mockMvc.perform(post("/api/v1/tasks/20/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorId\":2,\"content\":\"Hello\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("Author is not a member of this project"));
    }

    @Test
    void listReturnsTheCommentsAsAJsonArray() throws Exception {
        when(commentService.getByTask(20L))
                .thenReturn(List.of(response(30L, "first"), response(31L, "second")));

        mockMvc.perform(get("/api/v1/tasks/20/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("first"))
                .andExpect(jsonPath("$[1].content").value("second"));
    }

    @Test
    void listReturns404ForAMissingTask() throws Exception {
        when(commentService.getByTask(99L)).thenThrow(new ResourceNotFoundException("Task", 99L));

        mockMvc.perform(get("/api/v1/tasks/99/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found with id 99"))
                .andExpect(jsonPath("$.path").value("/api/v1/tasks/99/comments"));
    }

    @Test
    void updateReturnsTheEditedComment() throws Exception {
        when(commentService.update(eq(30L), any(UpdateCommentRequest.class)))
                .thenReturn(response(30L, "Edited"));

        mockMvc.perform(put("/api/v1/comments/30")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Edited\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Edited"));
    }

    @Test
    void deleteReturns204WithNoBody() throws Exception {
        mockMvc.perform(delete("/api/v1/comments/30"))
                .andExpect(status().isNoContent());

        verify(commentService).delete(30L);
    }
}