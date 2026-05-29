-- =============================================
-- V4__add_indexes.sql
-- Add indexes for better query performance
-- =============================================

-- Index for filtering active/inactive products
CREATE INDEX idx_products_active ON products (active);

-- Multi-column index for idempotency checks
DROP INDEX IF EXISTS idx_idempotency_key;
CREATE INDEX idx_idempotency_key_type ON idempotency_records (idempotency_key, operation_type);
