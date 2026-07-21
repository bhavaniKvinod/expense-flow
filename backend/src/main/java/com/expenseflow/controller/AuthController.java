package com.expenseflow.controller;

import com.expenseflow.dto.AuthDtos;
import com.expenseflow.dto.UserDtos;
import com.expenseflow.mapper.Mappers;
import com.expenseflow.security.CurrentUserProvider;
import com.expenseflow.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;

    public AuthController(AuthService authService, CurrentUserProvider currentUserProvider) {
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserDtos.UserResponse me() {
        return Mappers.toUserResponse(currentUserProvider.require());
    }
}
