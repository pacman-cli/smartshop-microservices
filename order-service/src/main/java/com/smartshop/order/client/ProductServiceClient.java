package com.smartshop.order.client;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Feign Client for communicating with Product Service.
 * Uses Eureka service discovery (name = "product-service").
 */
@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/api/products/{id}")
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    @Bulkhead(name = "productService")
    ProductResponse getProductById(@PathVariable("id") Long id);

    @GetMapping("/api/products/batch")
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductsByIdsFallback")
    @Bulkhead(name = "productService")
    List<ProductResponse> getProductsByIds(@RequestParam("ids") List<Long> ids);

    @PutMapping("/api/products/{id}/reduce-stock")
    @CircuitBreaker(name = "productService", fallbackMethod = "reduceStockFallback")
    @Bulkhead(name = "productService")
    ProductResponse reduceStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);

    @PutMapping("/api/products/{id}/restore-stock")
    @CircuitBreaker(name = "productService", fallbackMethod = "restoreStockFallback")
    @Bulkhead(name = "productService")
    ProductResponse restoreStock(@PathVariable("id") Long id, @RequestParam("quantity") int quantity);

    @PostMapping("/api/products/batch-reduce-stock")
    @CircuitBreaker(name = "productService", fallbackMethod = "batchReduceStockFallback")
    @Bulkhead(name = "productService")
    List<ProductResponse> batchReduceStock(@RequestBody BatchStockRequest request);

    @PostMapping("/api/products/batch-restore-stock")
    @CircuitBreaker(name = "productService", fallbackMethod = "batchRestoreStockFallback")
    @Bulkhead(name = "productService")
    List<ProductResponse> batchRestoreStock(@RequestBody BatchStockRequest request);

    default ProductResponse getProductFallback(Long id, Throwable t) {
        // Return a minimal degraded product instead of throwing
        return ProductResponse.builder()
                .id(id)
                .name("Product currently unavailable")
                .description("We are having trouble fetching product details. Please try again later.")
                .price(java.math.BigDecimal.ZERO)
                .active(false)
                .build();
    }

    default ProductResponse reduceStockFallback(Long id, int quantity, Throwable t) {
        throw new RuntimeException("Product service is unavailable. Cannot reduce stock for product: " + id, t);
    }

    default ProductResponse restoreStockFallback(Long id, int quantity, Throwable t) {
        throw new RuntimeException("Product service is unavailable. Cannot restore stock for product: " + id, t);
    }

    default List<ProductResponse> batchReduceStockFallback(BatchStockRequest request, Throwable t) {
        throw new RuntimeException("Product service is unavailable. Cannot batch reduce stock.", t);
    }

    default List<ProductResponse> batchRestoreStockFallback(BatchStockRequest request, Throwable t) {
        throw new RuntimeException("Product service is unavailable. Cannot batch restore stock.", t);
    }

    default List<ProductResponse> getProductsByIdsFallback(List<Long> ids, Throwable t) {
        return List.of(); // Return empty list as degraded response
    }
}
