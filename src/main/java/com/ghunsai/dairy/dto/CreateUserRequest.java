package com.ghunsai.dairy.dto;

import com.ghunsai.dairy.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "Username is required")
        @Pattern(regexp = "^[A-Za-z0-9._-]{3,50}$",
                message = "Username must be 3-50 characters: letters, digits, dot, underscore, dash") String username,
        @NotBlank(message = "Full name is required") @Size(max = 100) String fullName,
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters") String password,
        @NotNull(message = "Role is required") Role role) {
}
