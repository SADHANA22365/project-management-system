package com.company.pms.controller;

import com.company.pms.dto.request.CreateUserRequest;
import com.company.pms.dto.request.UpdateUserRequest;
import com.company.pms.dto.response.PageResponse;
import com.company.pms.dto.response.UserResponse;
import com.company.pms.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Users", description = "Create, update and (de)activate users")
@RestController
@RequestMapping("/api/v1/users")

public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Create a user",
        description = "Email must be unique ignoring case (409 if it already exists).")

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @GetMapping
    public PageResponse<UserResponse> getAll(Pageable pageable) {
        return userService.getAll(pageable);
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id,
                               @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @PatchMapping("/{id}/activate")
    public UserResponse activate(@PathVariable Long id) {
        return userService.activate(id);
    }

    @Operation(summary = "Deactivate a user",
        description = "Deactivated users cannot be assigned tasks, added to projects, "
                + "report tasks or write comments.")

    @PatchMapping("/{id}/deactivate")
    public UserResponse deactivate(@PathVariable Long id) {
        return userService.deactivate(id);
    }
}