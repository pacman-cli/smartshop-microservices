package com.smartshop.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request for atomic batch stock operations.
 * Either all items succeed or none do (rollback).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchStockRequest {

    @NotEmpty(message = "At least one stock item is required")
    @Valid
    private List<StockItem> items;
}
