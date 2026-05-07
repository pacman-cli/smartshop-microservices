-- =============================================
-- V3__add_indexes.sql
-- Add indexes for better query performance
-- =============================================

-- Index for filtering by user role
CREATE INDEX idx_users_role ON users (role);
