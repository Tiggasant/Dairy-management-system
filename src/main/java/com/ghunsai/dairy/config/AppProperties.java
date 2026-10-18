package com.ghunsai.dairy.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotBlank String timezone,
        @NotNull @Valid Jwt jwt,
        Cors cors,
        Bootstrap bootstrap) {

    public record Jwt(
            @NotBlank @Size(min = 32, message = "JWT_SECRET must be at least 32 characters") String secret,
            @Min(5) long expirationMinutes,
            @NotBlank String issuer) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Bootstrap(Owner owner) {
    }

    public record Owner(String username, String password, String fullName) {
    }
}
