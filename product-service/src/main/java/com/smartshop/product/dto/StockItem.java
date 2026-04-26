package com.smartshop.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single item in a batch stock operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockItem {

    @NotNull(message = "Product ID is required")
    @Min(value = 1, message = "Product ID must be positive")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
