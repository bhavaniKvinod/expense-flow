package com.expenseflow.dto;

import com.expenseflow.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UserDtos {

    public record UserResponse(
            Long id,
            String name,
            String email,
            Role role,
            Long managerId,
            String managerName,
            boolean active) {
    }

    public record CreateUserRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password,
            @NotNull Role role,
            Long managerId) {
    }

    public record UpdateUserRequest(
            @NotBlank String name,
            @NotNull Role role,
            Long managerId,
            boolean active) {
    }
}
