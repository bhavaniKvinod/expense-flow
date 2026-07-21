package com.expenseflow.service;

import com.expenseflow.dto.UserDtos;
import com.expenseflow.entity.User;
import com.expenseflow.exception.BadRequestException;
import com.expenseflow.exception.NotFoundException;
import com.expenseflow.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> listAll() {
        return userRepository.findAll();
    }

    @Transactional
    public User create(UserDtos.CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("A user with email " + request.email() + " already exists.");
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setManager(resolveManager(request.managerId()));
        user.setActive(true);
        return userRepository.save(user);
    }

    @Transactional
    public User update(Long userId, UserDtos.UpdateUserRequest request) {
        User user = findById(userId);
        user.setName(request.name());
        user.setRole(request.role());
        user.setManager(resolveManager(request.managerId()));
        user.setActive(request.active());
        return user;
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User " + userId + " not found"));
    }

    private User resolveManager(Long managerId) {
        if (managerId == null) {
            return null;
        }
        return userRepository.findById(managerId)
                .orElseThrow(() -> new NotFoundException("Manager " + managerId + " not found"));
    }
}
