package com.company.pms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(

        @NotBlank(message = "Comment content is required")
        @Size(max = 2000, message = "Comment must not exceed 2000 characters")
        String content
) {
}