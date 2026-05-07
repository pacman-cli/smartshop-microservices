-- =============================================
-- V3__add_audit_columns.sql
-- Add created_by and updated_by columns for auditing
-- =============================================

ALTER TABLE payments ADD COLUMN created_by VARCHAR(255) DEFAULT 'SYSTEM';
ALTER TABLE payments ADD COLUMN updated_by VARCHAR(255);
