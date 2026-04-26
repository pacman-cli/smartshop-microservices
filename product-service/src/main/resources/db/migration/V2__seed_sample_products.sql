-- =============================================
-- V2__seed_sample_products.sql
-- Product Service — Insert sample products for development
-- =============================================

INSERT INTO products (name, description, price, quantity, sku, category, active, created_at)
VALUES
    ('MacBook Pro 16"', 'Apple MacBook Pro with M3 Pro chip, 18GB RAM, 512GB SSD', 2499.99, 50, 'ELEC-MBP16-001', 'ELECTRONICS', TRUE, CURRENT_TIMESTAMP),
    ('iPhone 15 Pro', 'Apple iPhone 15 Pro 256GB Titanium', 1199.99, 200, 'ELEC-IP15P-001', 'ELECTRONICS', TRUE, CURRENT_TIMESTAMP),
    ('Sony WH-1000XM5', 'Wireless Noise Cancelling Headphones', 349.99, 150, 'ELEC-SNYWH5-001', 'ELECTRONICS', TRUE, CURRENT_TIMESTAMP),
    ('Levi''s 501 Jeans', 'Classic straight fit jeans, dark wash', 79.99, 300, 'CLTH-LV501-001', 'CLOTHING', TRUE, CURRENT_TIMESTAMP),
    ('Nike Air Max 90', 'Classic running shoes, white/black', 129.99, 250, 'CLTH-NAM90-001', 'CLOTHING', TRUE, CURRENT_TIMESTAMP),
    ('Clean Code', 'Robert C. Martin - A Handbook of Agile Software Craftsmanship', 39.99, 100, 'BOOK-CC-001', 'BOOKS', TRUE, CURRENT_TIMESTAMP),
    ('Designing Data-Intensive Applications', 'Martin Kleppmann - Big Ideas Behind Reliable Systems', 44.99, 80, 'BOOK-DDIA-001', 'BOOKS', TRUE, CURRENT_TIMESTAMP),
    ('Dyson V15 Detect', 'Cordless vacuum cleaner with laser dust detection', 749.99, 60, 'HOME-DYSV15-001', 'HOME', TRUE, CURRENT_TIMESTAMP),
    ('Yoga Mat Pro', 'Extra thick non-slip exercise mat, 6mm', 34.99, 400, 'SPRT-YGMT-001', 'SPORTS', TRUE, CURRENT_TIMESTAMP),
    ('LEGO Technic Porsche', 'LEGO Technic Porsche 911 GT3 RS, 2704 pieces', 159.99, 45, 'TOYS-LGPR-001', 'TOYS', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (sku) DO NOTHING;
