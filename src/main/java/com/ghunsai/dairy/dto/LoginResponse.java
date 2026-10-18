package com.ghunsai.dairy.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user) {
}
