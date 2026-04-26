package com.smartshop.order.entity;

/**
 * Order status lifecycle:
 * PENDING -> CONFIRMED -> SHIPPED -> DELIVERED
 * PENDING -> CANCELLED
 * PENDING -> CONFIRMED -> PAYMENT_FAILED
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PAYMENT_FAILED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
