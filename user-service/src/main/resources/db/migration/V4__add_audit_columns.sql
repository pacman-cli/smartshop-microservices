-- =============================================
-- V4__add_audit_columns.sql
-- Add created_by and updated_by columns for auditing
-- =============================================

ALTER TABLE users ADD COLUMN created_by VARCHAR(255) DEFAULT 'SYSTEM';
ALTER TABLE users ADD COLUMN updated_by VARCHAR(255);
