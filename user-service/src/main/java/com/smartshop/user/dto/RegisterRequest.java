package com.smartshop.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Registration Request DTO — what the client sends to POST /api/auth/register.
 *
 * Why a DTO instead of using the User entity directly?
 * 1. The entity has fields the client shouldn't set (id, createdAt, role)
 * 2. Validation annotations belong on DTOs, not entities
 * 3. If the entity changes, the API contract stays stable
 *
 * Validation annotations:
 * - @NotBlank → field must not be null or empty
 * - @Email    → must be a valid email format
 * - @Size     → min/max length constraints
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;
}
