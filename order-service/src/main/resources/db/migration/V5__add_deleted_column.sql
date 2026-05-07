-- =============================================
-- V5__add_deleted_column.sql
-- Add deleted column for soft delete support
-- =============================================

ALTER TABLE orders ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_orders_deleted ON orders (deleted);
