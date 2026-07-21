package com.expenseflow.controller;

import com.expenseflow.dto.UserDtos;
import com.expenseflow.mapper.Mappers;
import com.expenseflow.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDtos.UserResponse> list() {
        return userService.listAll().stream().map(Mappers::toUserResponse).toList();
    }

    @PostMapping
    public UserDtos.UserResponse create(@Valid @RequestBody UserDtos.CreateUserRequest request) {
        return Mappers.toUserResponse(userService.create(request));
    }

    @PutMapping("/{id}")
    public UserDtos.UserResponse update(@PathVariable Long id,
                                        @Valid @RequestBody UserDtos.UpdateUserRequest request) {
        return Mappers.toUserResponse(userService.update(id, request));
    }
}
