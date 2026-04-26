-- =============================================
-- V1__create_users_table.sql
-- User Service — Initial schema
-- =============================================

CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(100)    NOT NULL,
    email           VARCHAR(150)    NOT NULL,
    password        VARCHAR(255)    NOT NULL,
    role            VARCHAR(255)    NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,

    CONSTRAINT uk_users_email UNIQUE (email)
);

-- Index for sorting/filtering by creation date
CREATE INDEX idx_users_created_at ON users (created_at);

-- Index for email lookups (unique constraint already creates one, but explicit for clarity)
COMMENT ON TABLE users IS 'Stores registered users with BCrypt-hashed passwords';
