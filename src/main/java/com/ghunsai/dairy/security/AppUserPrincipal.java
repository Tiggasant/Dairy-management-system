package com.ghunsai.dairy.security;

import com.ghunsai.dairy.entity.Role;

/** The authenticated user, available in controllers via {@code @AuthenticationPrincipal}. */
public record AppUserPrincipal(Long id, String username, String fullName, Role role) {

    public boolean isOwner() {
        return role == Role.OWNER;
    }
}
