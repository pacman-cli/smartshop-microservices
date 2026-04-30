-- =============================================
-- V1__create_orders_tables.sql
-- Order Service — Initial schema (orders + order_items)
-- =============================================
-- NOTE: user_id and product_id reference logical relationships
-- to other services' data. No DB-level foreign keys due to
-- database-per-service pattern (microservices architecture).

-- =============================================
-- 1. Orders table
-- =============================================
CREATE TABLE orders (
    id              BIGSERIAL       PRIMARY KEY,
    order_number    VARCHAR(50)     NOT NULL,
    user_id         BIGINT          NOT NULL,
    user_email      VARCHAR(150),
    status          VARCHAR(255)    NOT NULL DEFAULT 'PENDING',
    total_amount    NUMERIC(12, 2)  NOT NULL,
    shipping_address VARCHAR(500),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,

    CONSTRAINT uk_orders_order_number UNIQUE (order_number)
);

-- Index for filtering orders by user
CREATE INDEX idx_orders_user_id ON orders (user_id);

-- Index for filtering orders by status
CREATE INDEX idx_orders_status ON orders (status);

-- =============================================
-- 2. Order Items table
-- =============================================
CREATE TABLE order_items (
    id              BIGSERIAL       PRIMARY KEY,
    order_id        BIGINT          NOT NULL,
    product_id      BIGINT          NOT NULL,
    product_name    VARCHAR(200)    NOT NULL,
    product_sku     VARCHAR(50),
    quantity        INTEGER         NOT NULL,
    price           NUMERIC(10, 2)  NOT NULL,

    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES orders (id)
        ON DELETE CASCADE
);

-- Index on order_id for join performance
CREATE INDEX idx_order_items_order_id ON order_items (order_id);

COMMENT ON TABLE orders IS 'Customer orders with status tracking';
COMMENT ON TABLE order_items IS 'Line items belonging to an order';
