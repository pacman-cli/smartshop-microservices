-- =============================================
-- V6__add_deleted_column.sql
-- Add deleted column for soft delete support
-- =============================================

ALTER TABLE products ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_products_deleted ON products (deleted);
