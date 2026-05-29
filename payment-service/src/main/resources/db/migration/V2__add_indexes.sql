-- =============================================
-- V2__add_indexes.sql
-- Add indexes for better query performance
-- =============================================

-- Index for filtering payments by user
CREATE INDEX idx_payments_user_id ON payments (user_id);

-- Index for date-range filtering of payments
CREATE INDEX idx_payments_created_at ON payments (created_at);
