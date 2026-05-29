package com.smartshop.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartshop.order.client.ProductResponse;
import com.smartshop.order.client.ProductServiceClient;
import com.smartshop.order.client.UserResponse;
import com.smartshop.order.client.UserServiceClient;
import com.smartshop.order.dto.OrderItemRequest;
import com.smartshop.order.dto.OrderRequest;
import com.smartshop.order.dto.OrderResponse;
import com.smartshop.order.entity.Order;
import com.smartshop.order.entity.OrderStatus;
import com.smartshop.order.exception.OrderNotFoundException;
import com.smartshop.order.repository.OrderRepository;
import com.smartshop.order.repository.OutboxRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductServiceClient productServiceClient;
    @Mock
    private UserServiceClient userServiceClient;
    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private ObjectMapper objectMapper;
    private MeterRegistry meterRegistry;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        orderService = new OrderService(
                orderRepository, productServiceClient, userServiceClient,
                outboxRepository, objectMapper, meterRegistry);

        // Set up security context for createOrder tests
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("100", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void getOrderByIdReturnsResponseWhenExists() {
        Order order = Order.builder()
                .id(1L)
                .userId(100L)
                .orderNumber("ORD-001")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("99.99"))
                .items(List.of())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse result = orderService.getOrderById(1L);

        assertThat(result.getOrderNumber()).isEqualTo("ORD-001");
        assertThat(result.getUserId()).isEqualTo(100L);
        verify(orderRepository).findById(1L);
    }

    @Test
    void getOrderByIdThrowsWhenNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getOrdersByUserIdReturnsPagedResults() {
        Order order = Order.builder()
                .id(1L).userId(100L).orderNumber("ORD-001")
                .status(OrderStatus.PENDING).totalAmount(new BigDecimal("50.00"))
                .items(List.of())
                .build();

        Page<Order> page = new PageImpl<>(List.of(order));
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(orderRepository.findByUserId(100L, pageRequest)).thenReturn(page);

        Page<OrderResponse> result = orderService.getOrdersByUserId(100L, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getOrderNumber()).isEqualTo("ORD-001");
    }

    @Test
    void createOrderGeneratesOrderNumber() throws Exception {
        OrderRequest request = OrderRequest.builder()
                .userId(100L)
                .shippingAddress("123 Main St")
                .items(List.of(OrderItemRequest.builder()
                        .productId(1L)
                        .quantity(2)
                        .build()))
                .build();

        UserResponse user = new UserResponse();
        user.setId(100L);
        user.setEmail("test@example.com");

        ProductResponse product = new ProductResponse();
        product.setId(1L);
        product.setName("Test Product");
        product.setSku("SKU-001");
        product.setPrice(new BigDecimal("25.00"));

        when(userServiceClient.getUserById(100L)).thenReturn(user);
        when(productServiceClient.getProductsByIds(List.of(1L))).thenReturn(List.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(1L);
            order.setCreatedAt(LocalDateTime.now());
            return order;
        });
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        OrderResponse result = orderService.createOrder(request);

        assertThat(result.getOrderNumber()).isNotNull();
        assertThat(result.getOrderNumber()).startsWith("ORD-");
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void getOrderByNumberReturnsResponse() {
        Order order = Order.builder()
                .id(1L).userId(100L).orderNumber("ORD-001")
                .status(OrderStatus.COMPLETED).totalAmount(new BigDecimal("75.00"))
                .items(List.of())
                .build();

        when(orderRepository.findByOrderNumber("ORD-001")).thenReturn(Optional.of(order));

        OrderResponse result = orderService.getOrderByNumber("ORD-001");

        assertThat(result.getOrderNumber()).isEqualTo("ORD-001");
        assertThat(result.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void updateOrderStatusChangesStatus() {
        // Set admin auth for this test
        UsernamePasswordAuthenticationToken adminAuth =
                new UsernamePasswordAuthenticationToken("100", null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(adminAuth);

        Order order = Order.builder()
                .id(1L).userId(100L).orderNumber("ORD-001")
                .status(OrderStatus.PENDING).totalAmount(new BigDecimal("50.00"))
                .items(List.of())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse result = orderService.updateOrderStatus(1L, OrderStatus.SHIPPED);

        assertThat(result.getStatus()).isEqualTo("SHIPPED");
    }

    @Test
    void cancelOrderSetsCancelledStatus() {
        Order order = Order.builder()
                .id(1L).userId(100L).orderNumber("ORD-001")
                .status(OrderStatus.PENDING).totalAmount(new BigDecimal("50.00"))
                .items(List.of())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderResponse result = orderService.cancelOrder(1L);

        assertThat(result.getStatus()).isEqualTo("CANCELLED");
    }
}
