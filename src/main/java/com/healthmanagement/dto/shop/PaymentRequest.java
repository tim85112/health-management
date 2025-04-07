package com.healthmanagement.dto.shop;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private String orderId;
    private BigDecimal amount;
    private String currency = "TWD";
    private String paymentMethod;
    private String description;
    private String returnUrl;
    private String notifyUrl;
} 