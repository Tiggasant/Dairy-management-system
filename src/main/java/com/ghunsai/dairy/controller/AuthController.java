package com.ghunsai.dairy.controller;

import com.ghunsai.dairy.dto.ChangePasswordRequest;
import com.ghunsai.dairy.dto.LoginRequest;
import com.ghunsai.dairy.dto.LoginResponse;
import com.ghunsai.dairy.dto.UserResponse;
import com.ghunsai.dairy.security.AppUserPrincipal;
import com.ghunsai.dairy.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return authService.login(request.username(), request.password(), http.getRemoteAddr());
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUserPrincipal user) {
        return authService.me(user.id());
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal AppUserPrincipal user,
                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.id(), request.currentPassword(), request.newPassword());
    }
}
