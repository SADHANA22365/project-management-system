package com.company.pms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateLabelRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 50, message = "Name must be at most 50 characters")
        String name,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$",
                message = "Colour code must look like #FF5733")
        String colourCode
) {
}