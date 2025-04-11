package com.healthmanagement.dto.shop;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponse {
    private String paymentId;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String method;
    private String status;
    private String paymentUrl;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
} 