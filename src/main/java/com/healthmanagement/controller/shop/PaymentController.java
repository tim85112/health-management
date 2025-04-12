package com.healthmanagement.controller.shop;

import com.healthmanagement.util.ApiResponse;
import com.healthmanagement.dto.shop.PaymentRequest;
import com.healthmanagement.dto.shop.PaymentResponse;
import com.healthmanagement.service.shop.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/{orderId}/payment")
    public ResponseEntity<?> createPayment(
            @PathVariable Integer orderId,
            @RequestBody PaymentRequest paymentRequest) {
        PaymentResponse response = paymentService.createPayment(orderId, paymentRequest);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/payment/{paymentId}/status")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String paymentId) {
        PaymentResponse response = paymentService.getPaymentStatus(paymentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/payment/{paymentId}/mock")
    public ResponseEntity<?> mockPaymentCallback(
            @PathVariable String paymentId,
            @RequestParam(required = false, defaultValue = "SUCCESS") String status) {
        boolean result = paymentService.mockPaymentCallback(paymentId, status);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
} 