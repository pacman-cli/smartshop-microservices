-- =============================================
-- V1__create_products_table.sql
-- Product Service — Initial schema
-- =============================================

CREATE TABLE products (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(200)    NOT NULL,
    description     VARCHAR(1000),
    price           NUMERIC(10, 2)  NOT NULL,
    quantity        INTEGER         NOT NULL,
    sku             VARCHAR(50)     NOT NULL,
    category        VARCHAR(255)    NOT NULL,
    image_url       VARCHAR(500),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP,

    CONSTRAINT uk_products_sku UNIQUE (sku)
);

-- Index for filtering by category
CREATE INDEX idx_products_category ON products (category);

-- Index for searching by name
CREATE INDEX idx_products_name ON products (name);

COMMENT ON TABLE products IS 'Product catalog with stock management';
