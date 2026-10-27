package com.company.pms.service.impl;

import com.company.pms.dto.request.CreateUserRequest;
import com.company.pms.dto.request.UpdateUserRequest;
import com.company.pms.dto.response.UserResponse;
import com.company.pms.entity.User;
import com.company.pms.enums.Designation;
import com.company.pms.exception.DuplicateResourceException;
import com.company.pms.exception.ResourceNotFoundException;
import com.company.pms.mapper.UserMapper;
import com.company.pms.repository.UserRepository;
import com.company.pms.support.TestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private static UserResponse responseFor(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(),
                user.getDesignation(), user.isActive(), null, null, null, null, null);
    }

    @Test
    void createSavesTheUserWhenTheEmailIsNew() {
        CreateUserRequest request = new CreateUserRequest(
                "Ann Lee", " ann.lee@example.com ", Designation.DEVELOPER, null, null, null);
        User entity = TestData.user(null, "Ann Lee");
        User saved = TestData.user(5L, "Ann Lee");
        UserResponse expected = responseFor(saved);

        when(userRepository.existsByEmailIgnoreCase("ann.lee@example.com")).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(entity);
        when(userRepository.save(entity)).thenReturn(saved);
        when(userMapper.toResponse(saved)).thenReturn(expected);

        UserResponse result = userService.create(request);

        assertThat(result).isSameAs(expected);
        verify(userRepository).save(entity);
    }

    @Test
    void createRejectsADuplicateEmail() {
        CreateUserRequest request = new CreateUserRequest(
                "Ann Lee", "ann.lee@example.com", Designation.DEVELOPER, null, null, null);
        when(userRepository.existsByEmailIgnoreCase("ann.lee@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("ann.lee@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    void getByIdThrowsNotFoundForAMissingUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found with id 99");
    }

    @Test
    void updateRejectsAnEmailUsedByAnotherUser() {
        User user = TestData.user(1L, "Ann Lee");
        UpdateUserRequest request = new UpdateUserRequest(
                "Ann Lee", "taken@example.com", Designation.DEVELOPER, null, null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmailIgnoreCaseAndIdNot("taken@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userMapper, never()).updateEntity(any(), any());
    }

    @Test
    void deactivateMakesTheUserInactive() {
        User user = TestData.user(1L, "Ann Lee");
        UserResponse expected = responseFor(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        UserResponse result = userService.deactivate(1L);

        assertThat(user.isActive()).isFalse();
        assertThat(result).isSameAs(expected);
    }

    @Test
    void activateMakesAnInactiveUserActiveAgain() {
        User user = TestData.user(1L, "Ann Lee");
        user.deactivate();
        UserResponse expected = responseFor(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        userService.activate(1L);

        assertThat(user.isActive()).isTrue();
    }
}