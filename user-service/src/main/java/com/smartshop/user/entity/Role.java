package com.smartshop.user.entity;

/**
 * Roles available in the SmartShop system.
 *
 * CUSTOMER — can browse products, place orders
 * ADMIN    — can manage products, view all orders, manage users
 *
 * Used for Role-Based Access Control (RBAC).
 * Example: Only ADMIN can call DELETE /api/products/{id}
 */
public enum Role {
    CUSTOMER,
    ADMIN
}
