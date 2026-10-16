package com.company.pms.service.impl;

import com.company.pms.dto.request.CreateUserRequest;
import com.company.pms.dto.request.UpdateUserRequest;
import com.company.pms.dto.response.PageResponse;
import com.company.pms.dto.response.UserResponse;
import com.company.pms.entity.User;
import com.company.pms.exception.DuplicateResourceException;
import com.company.pms.exception.ResourceNotFoundException;
import com.company.pms.mapper.UserMapper;
import com.company.pms.repository.UserRepository;
import com.company.pms.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = request.email().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("A user with email '" + email + "' already exists");
        }
        User saved = userRepository.save(userMapper.toEntity(request));
        log.info("Created user id={}", saved.getId());
        return userMapper.toResponse(saved);
    }

    @Override
    public UserResponse getById(Long id) {
        return userMapper.toResponse(findUserOrThrow(id));
    }

    @Override
    public PageResponse<UserResponse> getAll(Pageable pageable) {
        Page<UserResponse> page = userRepository.findAll(pageable).map(userMapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findUserOrThrow(id);
        String email = request.email().trim();
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new DuplicateResourceException("A user with email '" + email + "' already exists");
        }
        userMapper.updateEntity(user, request);
        log.info("Updated user id={}", id);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse activate(Long id) {
        User user = findUserOrThrow(id);
        user.activate();
        log.info("Activated user id={}", id);
        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse deactivate(Long id) {
        User user = findUserOrThrow(id);
        user.deactivate();
        log.info("Deactivated user id={}", id);
        return userMapper.toResponse(user);
    }

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}