package com.expenseflow.security;

import com.expenseflow.entity.User;
import com.expenseflow.exception.NotFoundException;
import com.expenseflow.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the currently authenticated {@link User} entity from the JWT-populated security context.
 */
@Component
public class CurrentUserProvider {

    private final UserRepository userRepository;

    public CurrentUserProvider(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtAuthFilter.JwtUser jwtUser)) {
            throw new NotFoundException("No authenticated user in context");
        }
        return userRepository.findById(jwtUser.id())
                .orElseThrow(() -> new NotFoundException("Authenticated user no longer exists"));
    }

    public Long requireId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof JwtAuthFilter.JwtUser jwtUser) {
            return jwtUser.id();
        }
        throw new NotFoundException("No authenticated user in context");
    }
}
