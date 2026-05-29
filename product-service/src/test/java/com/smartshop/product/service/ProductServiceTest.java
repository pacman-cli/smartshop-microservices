package com.smartshop.product.service;

import com.smartshop.product.dto.BatchStockRequest;
import com.smartshop.product.dto.ProductRequest;
import com.smartshop.product.dto.ProductResponse;
import com.smartshop.product.dto.StockItem;
import com.smartshop.product.entity.Category;
import com.smartshop.product.entity.Product;
import com.smartshop.product.entity.IdempotencyRecord;
import com.smartshop.product.exception.DuplicateSkuException;
import com.smartshop.product.exception.ProductNotFoundException;
import com.smartshop.product.repository.ProductRepository;
import com.smartshop.product.repository.IdempotencyRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @Mock
    private RedissonClient redissonClient;

    private SimpleMeterRegistry meterRegistry;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        productService = new ProductService(productRepository, idempotencyRepository, redissonClient, meterRegistry);
    }

    private Product createSampleProduct() {
        return Product.builder()
                .id(1L)
                .name("Test Product")
                .description("A test product")
                .price(new BigDecimal("29.99"))
                .quantity(100)
                .sku("TEST-001")
                .category(Category.ELECTRONICS)
                .active(true)
                .build();
    }

    // ==================== GET TESTS ====================

    @Test
    void getProductByIdReturnsProduct() {
        Product product = createSampleProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertThat(response.getName()).isEqualTo("Test Product");
        assertThat(response.getPrice()).isEqualByComparingTo("29.99");
        assertThat(response.getSku()).isEqualTo("TEST-001");
    }

    @Test
    void getProductByIdThrowsWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getProductBySkuReturnsProduct() {
        Product product = createSampleProduct();
        when(productRepository.findBySku("TEST-001")).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductBySku("test-001");

        assertThat(response.getSku()).isEqualTo("TEST-001");
        verify(productRepository).findBySku("TEST-001");
    }

    @Test
    void getProductBySkuNormalizesToUppercase() {
        Product product = createSampleProduct();
        when(productRepository.findBySku("TEST-001")).thenReturn(Optional.of(product));

        productService.getProductBySku("test-001");

        verify(productRepository).findBySku("TEST-001");
    }

    @Test
    void getProductBySkuThrowsWhenNotFound() {
        when(productRepository.findBySku("NOTFOUND")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductBySku("NOTFOUND"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("SKU");
    }

    @Test
    void getProductsByIdsReturnsAllProducts() {
        List<Product> products = List.of(
                createSampleProduct(),
                Product.builder().id(2L).name("Product 2").sku("TEST-002").price(new BigDecimal("19.99")).quantity(50).category(Category.ELECTRONICS).active(true).build()
        );
        when(productRepository.findAllById(List.of(1L, 2L))).thenReturn(products);

        List<ProductResponse> responses = productService.getProductsByIds(List.of(1L, 2L));

        assertThat(responses).hasSize(2);
    }

    @Test
    void getProductsByIdsThrowsWhenSomeNotFound() {
        Product product = createSampleProduct();
        when(productRepository.findAllById(List.of(1L, 999L))).thenReturn(List.of(product));

        assertThatThrownBy(() -> productService.getProductsByIds(List.of(1L, 999L)))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("999");
    }

    // ==================== CREATE TESTS ====================

    @Test
    void createProductSuccessfully() {
        ProductRequest request = ProductRequest.builder()
                .name("New Product")
                .description("New Description")
                .price(new BigDecimal("49.99"))
                .quantity(200)
                .sku("NEW-001")
                .category(Category.ELECTRONICS)
                .build();

        when(productRepository.existsBySku("NEW-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        ProductResponse response = productService.createProduct(request);

        assertThat(response.getName()).isEqualTo("New Product");
        assertThat(response.getSku()).isEqualTo("NEW-001");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProductThrowsOnDuplicateSku() {
        ProductRequest request = ProductRequest.builder()
                .name("Duplicate")
                .sku("EXISTING-001")
                .price(new BigDecimal("10.00"))
                .quantity(5)
                .category(Category.ELECTRONICS)
                .build();

        when(productRepository.existsBySku("EXISTING-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateSkuException.class);
    }

    @Test
    void createProductSavesSkuAsProvided() {
        ProductRequest request = ProductRequest.builder()
                .name("Product")
                .sku("abc-123")
                .price(new BigDecimal("10.00"))
                .quantity(5)
                .category(Category.ELECTRONICS)
                .build();

        when(productRepository.existsBySku("ABC-123")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        productService.createProduct(request);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        // SKU is saved as-is from request
        assertThat(productCaptor.getValue().getSku()).isEqualTo("abc-123");
    }

    // ==================== DELETE TESTS ====================

    @Test
    void deleteProductSuccessfully() {
        Product product = createSampleProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.deleteProduct(1L);

        verify(productRepository).delete(product);
    }

    @Test
    void deleteProductThrowsWhenNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(999L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    // ==================== BATCH OPERATIONS TESTS ====================

    @Test
    void batchReduceStockSkipsIfIdempotencyKeyExists() {
        BatchStockRequest request = new BatchStockRequest();
        request.setIdempotencyKey("order-123");
        request.setItems(List.of(new StockItem(1L, 5)));

        when(idempotencyRepository.existsByIdempotencyKeyAndOperationType("order-123", "REDUCE"))
                .thenReturn(true);

        Product product = createSampleProduct();
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product));

        List<ProductResponse> responses = productService.batchReduceStock(request);

        assertThat(responses).hasSize(1);
    }

    @Test
    void batchRestoreStockSkipsIfIdempotencyKeyExists() {
        BatchStockRequest request = new BatchStockRequest();
        request.setIdempotencyKey("order-123");
        request.setItems(List.of(new StockItem(1L, 5)));

        when(idempotencyRepository.existsByIdempotencyKeyAndOperationType("order-123", "RESTORE"))
                .thenReturn(true);

        Product product = createSampleProduct();
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(product));

        List<ProductResponse> responses = productService.batchRestoreStock(request);

        assertThat(responses).hasSize(1);
    }
}