package com.expenseflow.service;

import com.expenseflow.dto.AuthDtos;
import com.expenseflow.entity.User;
import com.expenseflow.exception.NotFoundException;
import com.expenseflow.mapper.Mappers;
import com.expenseflow.repository.UserRepository;
import com.expenseflow.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        // Throws BadCredentialsException on failure (handled globally as 401).
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new NotFoundException("User not found"));

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name(), user.getId());
        return new AuthDtos.LoginResponse(token, jwtService.getExpirationMinutes(), Mappers.toUserResponse(user));
    }
}
