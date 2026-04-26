package com.smartshop.product.controller;

import com.smartshop.product.dto.BatchStockRequest;
import com.smartshop.product.dto.PagedResponse;
import com.smartshop.product.dto.ProductRequest;
import com.smartshop.product.dto.ProductResponse;
import com.smartshop.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable @Min(1) Long id) {
        return productService.getProductById(id);
    }

    @GetMapping(params = "sku")
    public ProductResponse getProductBySku(@RequestParam String sku) {
        return productService.getProductBySku(sku);
    }

    @GetMapping
    public PagedResponse<ProductResponse> getProducts(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search) {

        Page<ProductResponse> result = productService.getProducts(page, size, category, search);

        return PagedResponse.<ProductResponse>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable @Min(1) Long id,
            @Valid @RequestBody ProductRequest request) {
        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable @Min(1) Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Internal endpoint for order-service to reduce stock.
     */
    @PutMapping("/{id}/reduce-stock")
    public ProductResponse reduceStock(
            @PathVariable @Min(1) Long id,
            @RequestParam @Min(1) int quantity) {
        return productService.reduceStock(id, quantity);
    }

    /**
     * Internal endpoint for order-service to restore stock
     * when an order is cancelled or payment fails.
     */
    @PutMapping("/{id}/restore-stock")
    public ProductResponse restoreStock(
            @PathVariable @Min(1) Long id,
            @RequestParam @Min(1) int quantity) {
        return productService.restoreStock(id, quantity);
    }

    /**
     * Internal endpoint for order-service to atomically reduce stock
     * for multiple products in a single request.
     * All-or-nothing: if any item fails, the entire batch is rolled back.
     */
    @PostMapping("/batch-reduce-stock")
    public List<ProductResponse> batchReduceStock(@Valid @RequestBody BatchStockRequest request) {
        return productService.batchReduceStock(request);
    }

    /**
     * Internal endpoint for order-service to atomically restore stock
     * for multiple products when an order is cancelled or payment fails.
     */
    @PostMapping("/batch-restore-stock")
    public List<ProductResponse> batchRestoreStock(@Valid @RequestBody BatchStockRequest request) {
        return productService.batchRestoreStock(request);
    }
}
