package com.ghunsai.dairy.service;

import com.ghunsai.dairy.dto.LoginResponse;
import com.ghunsai.dairy.dto.UserResponse;
import com.ghunsai.dairy.entity.User;
import com.ghunsai.dairy.exception.ApiException;
import com.ghunsai.dairy.repository.UserRepository;
import com.ghunsai.dairy.security.JwtTokenService;
import com.ghunsai.dairy.security.LoginAttemptService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AuthService {

    private static final String BAD_CREDENTIALS = "Invalid username or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;
    private final LoginAttemptService loginAttempts;
    /** Used to spend the same time on unknown usernames (prevents user enumeration by timing). */
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService tokenService,
                       LoginAttemptService loginAttempts) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.loginAttempts = loginAttempts;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional
    public LoginResponse login(String username, String password, String clientAddress) {
        String normalized = username.trim().toLowerCase(Locale.ROOT);
        String throttleKey = clientAddress + "|" + normalized;
        loginAttempts.assertNotLocked(throttleKey);

        User user = userRepository.findByUsername(normalized).orElse(null);
        boolean passwordOk = passwordEncoder.matches(password, user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !passwordOk || !user.isActive()) {
            loginAttempts.recordFailure(throttleKey);
            throw new ApiException.Unauthorized(BAD_CREDENTIALS);
        }

        loginAttempts.reset(throttleKey);
        user.setLastLoginAt(LocalDateTime.now());
        JwtTokenService.IssuedToken token = tokenService.issue(user);
        return new LoginResponse(token.value(), "Bearer", token.expiresInSeconds(), UserResponse.from(user));
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException.NotFound("User not found"));
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ApiException.BadRequest("Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ApiException.BadRequest("New password must be different from the current password");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.invalidateTokens(); // every other device must log in again
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ApiException.NotFound("User not found"));
    }
}
