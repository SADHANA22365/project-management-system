package com.company.pms.mapper;

import com.company.pms.dto.request.CreateLabelRequest;
import com.company.pms.dto.request.UpdateLabelRequest;
import com.company.pms.dto.response.LabelResponse;
import com.company.pms.entity.Label;
import org.springframework.stereotype.Component;

@Component
public class LabelMapper {

    public Label toEntity(CreateLabelRequest request) {
        return new Label(request.name().trim(), request.colourCode());
    }

    public LabelResponse toResponse(Label label) {
        return new LabelResponse(
                label.getId(),
                label.getName(),
                label.getColourCode(),
                label.getCreatedAt(),
                label.getUpdatedAt()
        );
    }

    public void updateEntity(Label label, UpdateLabelRequest request) {
        label.setName(request.name().trim());
        label.setColourCode(request.colourCode());
    }
}