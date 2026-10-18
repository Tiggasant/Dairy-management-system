package com.ghunsai.dairy.controller;

import com.ghunsai.dairy.dto.ActiveRequest;
import com.ghunsai.dairy.dto.CreateUserRequest;
import com.ghunsai.dairy.dto.ResetPasswordRequest;
import com.ghunsai.dairy.dto.UserResponse;
import com.ghunsai.dairy.security.AppUserPrincipal;
import com.ghunsai.dairy.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** OWNER only (enforced in SecurityConfig). */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return userService.create(request);
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.list();
    }

    @PutMapping("/{id}/active")
    public UserResponse setActive(@PathVariable Long id,
                                  @Valid @RequestBody ActiveRequest request,
                                  @AuthenticationPrincipal AppUserPrincipal actingUser) {
        return userService.setActive(id, request.active(), actingUser.id());
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(id, request.newPassword());
    }
}
