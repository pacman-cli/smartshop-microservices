package com.smartshop.user.repository;

import com.smartshop.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User Repository — provides database operations for the User entity.
 *
 * By extending JpaRepository, we get these methods FOR FREE:
 * - save(user)        → INSERT or UPDATE
 * - findById(id)      → SELECT by primary key
 * - findAll()         → SELECT all users
 * - deleteById(id)    → DELETE by primary key
 * - count()           → COUNT all users
 *
 * We add custom query methods below.
 * Spring Data JPA generates the SQL from the method name:
 * - findByEmailIgnoreCase(email)   → case-insensitive lookup by email
 * - existsByEmailIgnoreCase(email) → duplicate check that ignores email casing
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by email address.
     * Used during login to look up the user's account.
     */
    Optional<User> findByEmailIgnoreCase(String email);

    /**
     * Check if an email is already registered.
     * Used during registration to prevent duplicate accounts.
     */
    boolean existsByEmailIgnoreCase(String email);
}
