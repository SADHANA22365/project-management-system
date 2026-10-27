package com.company.pms.service;

import com.company.pms.dto.request.CreateCommentRequest;
import com.company.pms.dto.request.UpdateCommentRequest;
import com.company.pms.dto.response.CommentResponse;

import java.util.List;

public interface CommentService {

    CommentResponse create(Long taskId, CreateCommentRequest request);

    List<CommentResponse> getByTask(Long taskId);

    CommentResponse update(Long id, UpdateCommentRequest request);

    void delete(Long id);
}