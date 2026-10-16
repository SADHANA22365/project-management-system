package com.company.pms.service;

import com.company.pms.dto.request.CreateUserRequest;
import com.company.pms.dto.request.UpdateUserRequest;
import com.company.pms.dto.response.PageResponse;
import com.company.pms.dto.response.UserResponse;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse create(CreateUserRequest request);

    UserResponse getById(Long id);

    PageResponse<UserResponse> getAll(Pageable pageable);

    UserResponse update(Long id, UpdateUserRequest request);

    UserResponse activate(Long id);

    UserResponse deactivate(Long id);
}