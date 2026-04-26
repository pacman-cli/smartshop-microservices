package com.smartshop.order.controller;

import com.smartshop.order.dto.OrderRequest;
import com.smartshop.order.dto.OrderResponse;
import com.smartshop.order.dto.PagedResponse;
import com.smartshop.order.entity.OrderStatus;
import com.smartshop.order.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrderById(@PathVariable @Min(1) Long id) {
        return orderService.getOrderById(id);
    }

    @GetMapping(params = "orderNumber")
    public OrderResponse getOrderByNumber(@RequestParam String orderNumber) {
        return orderService.getOrderByNumber(orderNumber);
    }

    @GetMapping(params = "userId")
    public PagedResponse<OrderResponse> getOrdersByUserId(
            @RequestParam @Min(1) Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {

        Page<OrderResponse> result = orderService.getOrdersByUserId(userId, page, size);
        return toPagedResponse(result);
    }

    @GetMapping
    public PagedResponse<OrderResponse> getAllOrders(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size) {

        Page<OrderResponse> result = orderService.getAllOrders(page, size);
        return toPagedResponse(result);
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateOrderStatus(
            @PathVariable @Min(1) Long id,
            @RequestParam OrderStatus status) {
        return orderService.updateOrderStatus(id, status);
    }

    @PatchMapping("/{id}/cancel")
    public OrderResponse cancelOrder(@PathVariable @Min(1) Long id) {
        return orderService.cancelOrder(id);
    }

    private <T> PagedResponse<T> toPagedResponse(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
