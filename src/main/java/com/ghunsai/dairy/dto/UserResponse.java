package com.ghunsai.dairy.dto;

import com.ghunsai.dairy.entity.Role;
import com.ghunsai.dairy.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String fullName,
        Role role,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getFullName(), u.getRole(),
                u.isActive(), u.getCreatedAt(), u.getLastLoginAt());
    }
}
