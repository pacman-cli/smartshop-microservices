-- =============================================
-- V3__add_indexes.sql
-- Add indexes for better query performance
-- =============================================

-- Index for date-range filtering of orders
CREATE INDEX idx_orders_created_at ON orders (created_at);

-- Index for looking up items by product
CREATE INDEX idx_order_items_product_id ON order_items (product_id);

-- Composite index for outbox polling (find PENDING events ordered by age)
CREATE INDEX idx_outbox_status_created ON outbox_events (status, created_at);
