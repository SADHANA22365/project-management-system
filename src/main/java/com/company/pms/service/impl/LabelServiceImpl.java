package com.company.pms.service.impl;

import com.company.pms.dto.request.CreateLabelRequest;
import com.company.pms.dto.request.UpdateLabelRequest;
import com.company.pms.dto.response.LabelResponse;
import com.company.pms.dto.response.PageResponse;
import com.company.pms.entity.Label;
import com.company.pms.exception.BusinessRuleException;
import com.company.pms.exception.DuplicateResourceException;
import com.company.pms.exception.ResourceNotFoundException;
import com.company.pms.mapper.LabelMapper;
import com.company.pms.repository.LabelRepository;
import com.company.pms.repository.TaskRepository;
import com.company.pms.service.LabelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class LabelServiceImpl implements LabelService {

    private static final Logger log = LoggerFactory.getLogger(LabelServiceImpl.class);

    private final LabelRepository labelRepository;
    private final TaskRepository taskRepository;
    private final LabelMapper labelMapper;

    public LabelServiceImpl(LabelRepository labelRepository,
                            TaskRepository taskRepository,
                            LabelMapper labelMapper) {
        this.labelRepository = labelRepository;
        this.taskRepository = taskRepository;
        this.labelMapper = labelMapper;
    }

    @Override
    @Transactional
    public LabelResponse create(CreateLabelRequest request) {
        String name = request.name().trim();
        if (labelRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("A label named '" + name + "' already exists");
        }

        Label saved = labelRepository.save(labelMapper.toEntity(request));
        log.info("Created label id={}", saved.getId());
        return labelMapper.toResponse(saved);
    }

    @Override
    public PageResponse<LabelResponse> getAll(Pageable pageable) {
        Page<Label> labels = labelRepository.findAll(pageable);
        return PageResponse.from(labels.map(labelMapper::toResponse));
    }

    @Override
    public LabelResponse getById(Long id) {
        return labelMapper.toResponse(findLabelOrThrow(id));
    }

    @Override
    @Transactional
    public LabelResponse update(Long id, UpdateLabelRequest request) {
        Label label = findLabelOrThrow(id);
        String name = request.name().trim();
        if (labelRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateResourceException("A label named '" + name + "' already exists");
        }

        labelMapper.updateEntity(label, request);
        log.info("Updated label id={}", id);
        return labelMapper.toResponse(label);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Label label = findLabelOrThrow(id);
        long usedBy = taskRepository.countByLabelsId(id);
        if (usedBy > 0) {
            throw new BusinessRuleException("Label is used by " + usedBy
                    + " task(s) and cannot be deleted");
        }
        labelRepository.delete(label);
        log.info("Deleted label id={}", id);
    }

    private Label findLabelOrThrow(Long id) {
        return labelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Label", id));
    }
}