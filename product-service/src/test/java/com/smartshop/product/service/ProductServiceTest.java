package com.smartshop.product.service;

import com.smartshop.product.dto.ProductRequest;
import com.smartshop.product.entity.Category;
import com.smartshop.product.entity.Product;
import com.smartshop.product.exception.DuplicateSkuException;
import com.smartshop.product.exception.InsufficientStockException;
import com.smartshop.product.exception.ProductNotFoundException;
import com.smartshop.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

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
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void getProductByIdReturnsProduct() {
        Product product = createSampleProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        var response = productService.getProductById(1L);

        assertThat(response.getName()).isEqualTo("Test Product");
        assertThat(response.getPrice()).isEqualByComparingTo("29.99");
    }

    @Test
    void getProductByIdThrowsWhenNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createProductThrowsOnDuplicateSku() {
        when(productRepository.existsBySku("TEST-001")).thenReturn(true);

        ProductRequest request = ProductRequest.builder()
                .name("Duplicate")
                .sku("test-001")
                .price(new BigDecimal("10.00"))
                .quantity(5)
                .category(Category.ELECTRONICS)
                .build();

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateSkuException.class);
    }

    @Test
    void reduceStockThrowsWhenInsufficientStock() {
        Product product = createSampleProduct();
        product.setQuantity(5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.reduceStock(1L, 10))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void reduceStockSuccessfully() {
        Product product = createSampleProduct();
        product.setQuantity(50);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        var response = productService.reduceStock(1L, 10);

        assertThat(product.getQuantity()).isEqualTo(40);
        verify(productRepository).save(product);
    }
}
