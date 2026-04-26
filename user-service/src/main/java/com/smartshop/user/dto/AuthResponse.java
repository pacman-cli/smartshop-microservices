package com.smartshop.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Auth Response DTO — returned after successful login or registration.
 *
 * Contains the JWT token that the client must include in subsequent requests:
 *   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
 *
 * We also return basic user info so the frontend can display
 * the user's name and role without making another API call.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private String email;
    private String name;
    private String role;
    private String message;
}
