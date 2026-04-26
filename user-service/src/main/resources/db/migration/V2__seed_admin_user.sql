-- =============================================
-- V2__seed_admin_user.sql
-- User Service — Insert default admin user
-- =============================================
-- Password: admin123 (BCrypt hash)
INSERT INTO users (name, email, password, role, created_at)
VALUES (
    'Admin User',
    'admin@smartshop.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN',
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;
