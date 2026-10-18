package com.ghunsai.dairy.service;

import com.ghunsai.dairy.dto.CreateUserRequest;
import com.ghunsai.dairy.dto.UserResponse;
import com.ghunsai.dairy.entity.Role;
import com.ghunsai.dairy.entity.User;
import com.ghunsai.dairy.exception.ApiException;
import com.ghunsai.dairy.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** Owner-only account management (create workers, disable them, reset passwords). */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(username)) {
            throw new ApiException.Conflict("Username already exists");
        }
        User user = new User(username, request.fullName().trim(),
                passwordEncoder.encode(request.password()), request.role());
        return UserResponse.from(userRepository.save(user));
    }

    public List<UserResponse> list() {
        return userRepository.findAllByOrderByUsernameAsc().stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse setActive(Long targetId, boolean active, Long actingUserId) {
        User user = find(targetId);
        if (!active) {
            if (user.getId().equals(actingUserId)) {
                throw new ApiException.BadRequest("You cannot deactivate your own account");
            }
            if (user.getRole() == Role.OWNER && user.isActive()
                    && userRepository.countByRoleAndActiveTrue(Role.OWNER) <= 1) {
                throw new ApiException.BadRequest("At least one active owner is required");
            }
            user.invalidateTokens(); // kicks the user out immediately
        }
        user.setActive(active);
        return UserResponse.from(user);
    }

    @Transactional
    public void resetPassword(Long targetId, String newPassword) {
        User user = find(targetId);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.invalidateTokens();
    }

    private User find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ApiException.NotFound("User not found"));
    }
}
