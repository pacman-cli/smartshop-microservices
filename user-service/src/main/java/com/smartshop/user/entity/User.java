package com.smartshop.user.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;


/**
 * User Entity — maps to the "users" table in PostgreSQL (user_db).
 *
 * JPA Annotations:
 * - @Entity    → Tells Hibernate "this class is a database table"
 * - @Table     → Specifies the table name (we use "users" not "user" because
 *                "user" is a reserved keyword in PostgreSQL)
 * - @Id        → Primary key
 * - @GeneratedValue → Auto-generate IDs (1, 2, 3, ...)
 * - @Column    → Maps field to a column with constraints
 * - @Enumerated → Stores enum as a string ("CUSTOMER") not a number (0)
 *
 * Lombok Annotations:
 * - @Data      → Generates getters, setters, toString, equals, hashCode
 * - @Builder   → Enables User.builder().name("John").email("john@mail.com").build()
 * - @NoArgsConstructor  → Required by JPA (Hibernate needs an empty constructor)
 * - @AllArgsConstructor → Used by @Builder
 */
@Entity
@Table(
    name = "users",
    indexes = {
        @Index(name = "idx_users_created_at", columnList = "created_at")
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "password")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;  // Stored as BCrypt hash, never plain text

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (name != null) {
            name = name.trim();
        }
        if (email != null) {
            email = email.trim().toLowerCase(java.util.Locale.ROOT);
        }
    }
}
