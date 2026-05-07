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
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    PAYMENT_FAILED,
    COMPLETED
}
