package com.company.pms.service;

import com.company.pms.dto.request.CreateLabelRequest;
import com.company.pms.dto.request.UpdateLabelRequest;
import com.company.pms.dto.response.LabelResponse;
import com.company.pms.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

public interface LabelService {

    LabelResponse create(CreateLabelRequest request);

    PageResponse<LabelResponse> getAll(Pageable pageable);

    LabelResponse getById(Long id);

    LabelResponse update(Long id, UpdateLabelRequest request);

    void delete(Long id);
}