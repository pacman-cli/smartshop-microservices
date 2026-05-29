package com.smartshop.product.service;

import com.smartshop.product.dto.BatchStockRequest;
import com.smartshop.product.dto.ProductRequest;
import com.smartshop.product.dto.ProductResponse;
import com.smartshop.product.dto.StockItem;
import com.smartshop.product.entity.Category;
import com.smartshop.product.entity.Product;
import com.smartshop.product.entity.IdempotencyRecord;
import com.smartshop.product.exception.DuplicateSkuException;
import com.smartshop.product.exception.InsufficientStockException;
import com.smartshop.product.exception.ProductNotFoundException;
import com.smartshop.product.repository.ProductRepository;
import com.smartshop.product.repository.IdempotencyRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ProductService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductRepository productRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final RedissonClient redissonClient;
    private final MeterRegistry meterRegistry;

    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with id: {}", id);
        Product product = findProductOrThrow(id);
        return mapToResponse(product);
    }

    public List<ProductResponse> getProductsByIds(List<Long> ids) {
        log.info("Fetching products with ids: {}", ids);
        List<Product> products = productRepository.findAllById(ids);
        if (products.size() != ids.size()) {
            List<Long> foundIds = products.stream().map(Product::getId).toList();
            List<Long> missing = ids.stream().filter(id -> !foundIds.contains(id)).toList();
            throw new ProductNotFoundException("Products not found with IDs: " + missing);
        }
        return products.stream().map(this::mapToResponse).toList();
    }

    public ProductResponse getProductBySku(String sku) {
        log.info("Fetching product with sku: {}", sku);
        Product product = productRepository.findBySku(sku.trim().toUpperCase())
                .orElseThrow(() -> new ProductNotFoundException("Product not found with SKU: " + sku));
        return mapToResponse(product);
    }

    public Page<ProductResponse> getProducts(int page, int size, String categoryFilter, String search) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        PageRequest pageRequest = PageRequest.of(normalizedPage, normalizedSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Product> products;

        if (search != null && !search.isBlank()) {
            products = productRepository.searchByKeyword(search.trim(), pageRequest);
        } else if (categoryFilter != null && !categoryFilter.isBlank()) {
            Category category = Category.valueOf(categoryFilter.trim().toUpperCase());
            products = productRepository.findByCategoryAndActiveTrue(category, pageRequest);
        } else {
            products = productRepository.findByActiveTrue(pageRequest);
        }

        return products.map(this::mapToResponse);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku().trim().toUpperCase())) {
            throw new DuplicateSkuException("Product with SKU already exists: " + request.getSku());
        }

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .sku(request.getSku())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .active(true)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created: {} (id={})", savedProduct.getName(), savedProduct.getId());

        return mapToResponse(savedProduct);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = findProductOrThrow(id);

        // Check for SKU conflict if SKU is being changed
        String newSku = request.getSku().trim().toUpperCase();
        if (!product.getSku().equals(newSku) && productRepository.existsBySku(newSku)) {
            throw new DuplicateSkuException("Product with SKU already exists: " + request.getSku());
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setQuantity(request.getQuantity());
        product.setSku(request.getSku());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated: {} (id={})", updatedProduct.getName(), updatedProduct.getId());

        return mapToResponse(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProductOrThrow(id);
        productRepository.delete(product); // This triggers @SQLDelete soft delete
        log.info("Product soft-deleted: {} (id={})", product.getName(), product.getId());
    }

    /**
     * Reduce stock for a product. Called by order-service via Feign.
     */
    @Transactional
    public ProductResponse reduceStock(Long productId, int quantity) {
        RLock lock = redissonClient.getLock("lock:product:stock:" + productId);
        lock.lock();
        try {
            Product product = findProductOrThrow(productId);

            if (!product.hasStock(quantity)) {
                throw new InsufficientStockException(
                        "Insufficient stock for product: " + product.getName() +
                        " (available: " + product.getQuantity() + ", requested: " + quantity + ")");
            }

            product.reduceStock(quantity);
            Product updated = productRepository.save(product);
            log.info("Stock reduced for product {} by {} units (remaining: {})",
                    product.getName(), quantity, updated.getQuantity());

            // Metrics
            meterRegistry.counter("smartshop.products.stock.reduced", "product", product.getSku()).increment(quantity);
            if (updated.getQuantity() < LOW_STOCK_THRESHOLD) {
                meterRegistry.counter("smartshop.products.stock.low", "product", product.getSku()).increment();
            }

            return mapToResponse(updated);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * Restore stock for a product. Called by order-service when an order
     * is cancelled or payment fails, to return reserved stock.
     */
    @Transactional
    public ProductResponse restoreStock(Long productId, int quantity) {
        RLock lock = redissonClient.getLock("lock:product:stock:" + productId);
        lock.lock();
        try {
            Product product = findProductOrThrow(productId);

            product.setQuantity(product.getQuantity() + quantity);
            Product updated = productRepository.save(product);
            log.info("Stock restored for product {} by {} units (new total: {})",
                    product.getName(), quantity, updated.getQuantity());

            return mapToResponse(updated);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * Atomically reduce stock for multiple products in a single transaction.
     */
    @Transactional
    public List<ProductResponse> batchReduceStock(BatchStockRequest request) {
        log.info("Batch reducing stock for {} items (key: {})", 
                request.getItems().size(), request.getIdempotencyKey());

        if (request.getIdempotencyKey() != null && 
            idempotencyRepository.existsByIdempotencyKeyAndOperationType(request.getIdempotencyKey(), "REDUCE")) {
            log.info("Stock already reduced for key: {}. Skipping.", request.getIdempotencyKey());
            List<Long> ids = request.getItems().stream().map(StockItem::getProductId).toList();
            return productRepository.findAllById(ids).stream().map(this::mapToResponse).toList();
        }

        List<Long> productIds = request.getItems().stream()
                .map(StockItem::getProductId)
                .distinct()
                .sorted()
                .toList();

        List<RLock> locks = productIds.stream()
                .map(id -> redissonClient.getLock("lock:product:stock:" + id))
                .toList();

        try {
            for (RLock lock : locks) {
                lock.lock();
            }

            List<Product> products = productRepository.findAllById(productIds);
            Map<Long, Product> productMap = products.stream()
                    .collect(Collectors.toMap(Product::getId, p -> p));

            for (StockItem item : request.getItems()) {
                Product product = productMap.get(item.getProductId());
                if (product == null) {
                    throw new ProductNotFoundException("Product not found with id: " + item.getProductId());
                }
                if (!product.hasStock(item.getQuantity())) {
                    throw new InsufficientStockException(
                            "Insufficient stock for product: " + product.getName() +
                            " (available: " + product.getQuantity() + ", requested: " + item.getQuantity() + ")");
                }
            }

            List<ProductResponse> results = new ArrayList<>();
            for (StockItem item : request.getItems()) {
                Product product = productMap.get(item.getProductId());
                product.reduceStock(item.getQuantity());
                Product updated = productRepository.save(product);
                log.info("Batch: stock reduced for product {} by {} units (remaining: {})",
                        product.getName(), item.getQuantity(), updated.getQuantity());
                results.add(mapToResponse(updated));

                // Metrics
                meterRegistry.counter("smartshop.products.stock.reduced", "product", product.getSku()).increment(item.getQuantity());
                if (updated.getQuantity() < LOW_STOCK_THRESHOLD) {
                    meterRegistry.counter("smartshop.products.stock.low", "product", product.getSku()).increment();
                }
            }

            if (request.getIdempotencyKey() != null) {
                idempotencyRepository.save(IdempotencyRecord.builder()
                        .idempotencyKey(request.getIdempotencyKey())
                        .operationType("REDUCE")
                        .build());
            }

            return results;
        } finally {
            for (int i = locks.size() - 1; i >= 0; i--) {
                RLock lock = locks.get(i);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * Atomically restore stock for multiple products in a single transaction.
     */
    @Transactional
    public List<ProductResponse> batchRestoreStock(BatchStockRequest request) {
        log.info("Batch restoring stock for {} items (key: {})", 
                request.getItems().size(), request.getIdempotencyKey());

        if (request.getIdempotencyKey() != null && 
            idempotencyRepository.existsByIdempotencyKeyAndOperationType(request.getIdempotencyKey(), "RESTORE")) {
            log.info("Stock already restored for key: {}. Skipping.", request.getIdempotencyKey());
            List<Long> ids = request.getItems().stream().map(StockItem::getProductId).toList();
            return productRepository.findAllById(ids).stream().map(this::mapToResponse).toList();
        }

        List<Long> productIds = request.getItems().stream()
                .map(StockItem::getProductId)
                .distinct()
                .sorted()
                .toList();

        List<RLock> locks = productIds.stream()
                .map(id -> redissonClient.getLock("lock:product:stock:" + id))
                .toList();

        try {
            for (RLock lock : locks) {
                lock.lock();
            }

            List<ProductResponse> results = new ArrayList<>();
            for (StockItem item : request.getItems()) {
                Product product = findProductOrThrow(item.getProductId());
                product.setQuantity(product.getQuantity() + item.getQuantity());
                Product updated = productRepository.save(product);
                log.info("Batch: stock restored for product {} by {} units (new total: {})",
                        product.getName(), item.getQuantity(), updated.getQuantity());
                results.add(mapToResponse(updated));
            }

            if (request.getIdempotencyKey() != null) {
                idempotencyRepository.save(IdempotencyRecord.builder()
                        .idempotencyKey(request.getIdempotencyKey())
                        .operationType("RESTORE")
                        .build());
            }

            return results;
        } finally {
            for (int i = locks.size() - 1; i >= 0; i--) {
                RLock lock = locks.get(i);
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .quantity(product.getQuantity())
                .sku(product.getSku())
                .category(product.getCategory().name())
                .imageUrl(product.getImageUrl())
                .active(product.getActive())
                .createdAt(product.getCreatedAt())
                .build();
    }
}
