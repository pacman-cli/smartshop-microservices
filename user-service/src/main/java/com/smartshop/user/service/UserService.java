package com.smartshop.user.service;

import com.smartshop.user.dto.UserResponse;
import com.smartshop.user.entity.User;
import com.smartshop.user.exception.UserNotFoundException;
import com.smartshop.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User Service — business logic for user operations.
 *
 * This class handles:
 * - Fetching user profiles
 * - Listing all users (admin only, later)
 * - Converting entities to DTOs
 *
 * Authentication logic (register, login, JWT) will be added in Step 7.
 *
 * @RequiredArgsConstructor — Lombok generates a constructor with all
 *                            final fields, enabling dependency injection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;

    /**
     * Get a user by ID. Returns a UserResponse DTO (no password).
     *
     * @throws UserNotFoundException if no user with the given ID exists
     */
    @Cacheable(cacheNames = "usersById", key = "#id")
    public UserResponse getUserById(Long id) {
        log.info("Fetching user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found with id: " + id));

        return mapToUserResponse(user);
    }

    /**
     * Get a user by email. Used by other services via Feign.
     *
     * @throws UserNotFoundException if no user with the given email exists
     */
    @Cacheable(cacheNames = "usersByEmail", key = "#email == null ? '' : #email.trim().toLowerCase()")
    public UserResponse getUserByEmail(String email) {
        String normalizedEmail = normalizeEmail(email);

        log.info("Fetching user with email: {}", normalizedEmail);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new UserNotFoundException(
                        "User not found with email: " + normalizedEmail));

        return mapToUserResponse(user);
    }

    /**
     * Get users in a bounded page to avoid unbounded full-table scans.
     */
    public Page<UserResponse> getUsers(int page, int size) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        log.info("Fetching users page={} size={}", normalizedPage, normalizedSize);

        return userRepository.findAll(
                        PageRequest.of(
                                normalizedPage,
                                normalizedSize,
                                Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .map(this::mapToUserResponse);
    }

    /**
     * Legacy convenience method for callers that still expect a list.
     * Returns only the first bounded page to protect the service from
     * accidental full-table scans.
     */
    public List<UserResponse> getAllUsers() {
        return getUsers(0, DEFAULT_PAGE_SIZE).stream()
                .collect(Collectors.toList());
    }

    /**
     * Convert User entity → UserResponse DTO.
     * Strips out sensitive fields like password.
     */
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
