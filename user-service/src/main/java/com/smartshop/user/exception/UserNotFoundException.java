package com.smartshop.user.exception;

/**
 * Thrown when a user is not found by ID or email.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
