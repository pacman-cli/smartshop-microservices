-- =============================================
-- V3__create_idempotency_table.sql
-- Product Service — Idempotency for stock operations
-- =============================================

CREATE TABLE idempotency_records (
    id              UUID            PRIMARY KEY,
    idempotency_key VARCHAR(100)    NOT NULL,
    operation_type  VARCHAR(50)     NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_idempotency_key_type UNIQUE (idempotency_key, operation_type)
);

CREATE INDEX idx_idempotency_key ON idempotency_records (idempotency_key);

COMMENT ON TABLE idempotency_records IS 'Ensures stock operations are not applied twice for the same request';
