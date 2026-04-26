package com.smartshop.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Product Entity — maps to the "products" table in PostgreSQL (product_db).
 */
@Entity
@Table(
    name = "products",
    indexes = {
        @Index(name = "idx_products_category", columnList = "category"),
        @Index(name = "idx_products_name", columnList = "name"),
        @Index(name = "idx_products_sku", columnList = "sku", unique = true)
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(length = 500)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void normalizeFields() {
        if (name != null) {
            name = name.trim();
        }
        if (sku != null) {
            sku = sku.trim().toUpperCase();
        }
    }

    /**
     * Check if the product has enough stock for the requested quantity.
     */
    public boolean hasStock(int requestedQuantity) {
        return this.quantity != null && this.quantity >= requestedQuantity;
    }

    /**
     * Reduce stock by the given quantity. Throws if insufficient stock.
     */
    public void reduceStock(int amount) {
        if (!hasStock(amount)) {
            throw new IllegalStateException(
                    "Insufficient stock for product: " + name + " (available: " + quantity + ", requested: " + amount + ")");
        }
        this.quantity -= amount;
    }
}
