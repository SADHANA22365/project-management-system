package com.company.pms.dto.response;

import java.time.LocalDateTime;

public record LabelResponse(
        Long id,
        String name,
        String colourCode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}