package com.healthmanagement.controller.shop;

import com.healthmanagement.dto.shop.OrderDTO;
import com.healthmanagement.dto.shop.PaymentRequest;
import com.healthmanagement.dto.shop.PaymentResponse;
import com.healthmanagement.dto.shop.CreateOrderRequest;
import com.healthmanagement.service.shop.OrderService;
import com.healthmanagement.service.shop.PaymentService;
import com.healthmanagement.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/order")
@Tag(name = "Order", description = "Order management APIs")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Create order from cart", description = "Create a new order from user's cart items")
    public ResponseEntity<ApiResponse<OrderDTO>> createOrderFromCart(@RequestBody CreateOrderRequest request) {
        try {
            OrderDTO order = orderService.createOrderFromCart(request.getUserId());
            return ResponseEntity.ok(ApiResponse.success(order));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order by ID", description = "Retrieve order details by order ID")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(@PathVariable Integer orderId) {
        try {
            OrderDTO order = orderService.getOrderById(orderId);
            return ResponseEntity.ok(ApiResponse.success(order));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Get orders", description = "Retrieve orders with optional filters")
    public ResponseEntity<ApiResponse<List<OrderDTO>>> getOrders(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            List<OrderDTO> orders;
            if (userId != null) {
                orders = orderService.getOrdersByUserId(userId);
            } else {
                // 未來可以實現根據其他條件查詢訂單
                orders = Collections.emptyList();
            }
            return ResponseEntity.ok(ApiResponse.success(orders));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{orderId}/payment")
    @Operation(summary = "Create payment for order", description = "Create a payment request for an order")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPayment(
            @PathVariable Integer orderId,
            @RequestBody PaymentRequest request) {
        try {
            PaymentResponse response = paymentService.createPayment(request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/payment/{paymentId}/status")
    @Operation(summary = "Get payment status", description = "Query payment status by payment ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatus(@PathVariable String paymentId) {
        try {
            PaymentResponse response = paymentService.queryPaymentStatus(paymentId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/payment/{paymentId}/mock")
    @Operation(summary = "Mock payment callback", description = "Simulate payment callback for testing")
    public ResponseEntity<ApiResponse<Boolean>> mockPaymentCallback(@PathVariable String paymentId) {
        try {
            boolean result = paymentService.handlePaymentCallback(paymentId);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}