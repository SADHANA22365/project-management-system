package com.company.pms.mapper;

import com.company.pms.dto.request.CreateUserRequest;
import com.company.pms.dto.request.UpdateUserRequest;
import com.company.pms.dto.response.UserResponse;
import com.company.pms.entity.User;
import com.company.pms.entity.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public User toEntity(CreateUserRequest request) {
        User user = new User(request.fullName().trim(), request.email(), request.designation());
        if (hasProfileData(request.phoneNumber(), request.bio(), request.location())) {
            user.setProfile(new UserProfile(
                    request.phoneNumber(), request.bio(), request.location()));
        }
        return user;
    }

    public void updateEntity(User user, UpdateUserRequest request) {
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email());
        user.setDesignation(request.designation());

        if (hasProfileData(request.phoneNumber(), request.bio(), request.location())) {
            UserProfile profile = user.getProfile();
            if (profile == null) {
                user.setProfile(new UserProfile(
                        request.phoneNumber(), request.bio(), request.location()));
            } else {
                profile.setPhoneNumber(request.phoneNumber());
                profile.setBio(request.bio());
                profile.setLocation(request.location());
            }
        } else {
            user.setProfile(null);
        }
    }

    public UserResponse toResponse(User user) {
        UserProfile profile = user.getProfile();
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getDesignation(),
                user.isActive(),
                profile == null ? null : profile.getPhoneNumber(),
                profile == null ? null : profile.getBio(),
                profile == null ? null : profile.getLocation(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    private boolean hasProfileData(String phoneNumber, String bio, String location) {
        return isPresent(phoneNumber) || isPresent(bio) || isPresent(location);
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}