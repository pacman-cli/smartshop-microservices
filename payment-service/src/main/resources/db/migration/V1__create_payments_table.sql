-- =============================================
-- V1__create_payments_table.sql
-- Payment Service — Initial schema
-- =============================================

CREATE TABLE payments (
    id              BIGSERIAL       PRIMARY KEY,
    transaction_id  VARCHAR(50)     NOT NULL,
    order_number    VARCHAR(50)     NOT NULL,
    user_id         BIGINT          NOT NULL,
    amount          NUMERIC(12, 2)  NOT NULL,
    status          VARCHAR(255)    NOT NULL DEFAULT 'PENDING',
    payment_method  VARCHAR(255)    NOT NULL,
    failure_reason  VARCHAR(500),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,

    CONSTRAINT uk_payments_transaction_id UNIQUE (transaction_id)
);

-- Index for looking up payments by order
CREATE INDEX idx_payments_order_number ON payments (order_number);

-- Index for filtering by payment status
CREATE INDEX idx_payments_status ON payments (status);

COMMENT ON TABLE payments IS 'Payment records with transaction tracking';
