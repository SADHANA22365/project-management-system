package com.company.pms.controller;

import com.company.pms.dto.request.CreateCommentRequest;
import com.company.pms.dto.request.UpdateCommentRequest;
import com.company.pms.dto.response.CommentResponse;
import com.company.pms.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/tasks/{taskId}/comments")
    public ResponseEntity<CommentResponse> create(@PathVariable Long taskId,
                                                  @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.create(taskId, request));
    }

    @GetMapping("/tasks/{taskId}/comments")
    public List<CommentResponse> getByTask(@PathVariable Long taskId) {
        return commentService.getByTask(taskId);
    }

    @PutMapping("/comments/{id}")
    public CommentResponse update(@PathVariable Long id,
                                  @Valid @RequestBody UpdateCommentRequest request) {
        return commentService.update(id, request);
    }

    @DeleteMapping("/comments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}