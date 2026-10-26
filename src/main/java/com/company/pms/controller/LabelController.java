package com.company.pms.controller;

import com.company.pms.dto.request.CreateLabelRequest;
import com.company.pms.dto.request.UpdateLabelRequest;
import com.company.pms.dto.response.LabelResponse;
import com.company.pms.dto.response.PageResponse;
import com.company.pms.service.LabelService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LabelResponse create(@Valid @RequestBody CreateLabelRequest request) {
        return labelService.create(request);
    }

    @GetMapping
    public PageResponse<LabelResponse> getAll(@PageableDefault(sort = "name") Pageable pageable) {
        return labelService.getAll(pageable);
    }

    @GetMapping("/{id}")
    public LabelResponse getById(@PathVariable Long id) {
        return labelService.getById(id);
    }

    @PutMapping("/{id}")
    public LabelResponse update(@PathVariable Long id,
                                @Valid @RequestBody UpdateLabelRequest request) {
        return labelService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        labelService.delete(id);
    }
}