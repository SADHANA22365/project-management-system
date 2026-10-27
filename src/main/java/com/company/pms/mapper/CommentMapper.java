package com.company.pms.mapper;

import com.company.pms.dto.response.CommentResponse;
import com.company.pms.entity.Comment;
import com.company.pms.entity.User;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public CommentResponse toResponse(Comment comment) {
        User author = comment.getAuthor();
        return new CommentResponse(
                comment.getId(),
                comment.getTask().getId(),
                author.getId(),
                author.getFullName(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }
}