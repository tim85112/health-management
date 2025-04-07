package com.healthmanagement.dto.shop;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentResponse {
    private String paymentId;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String paymentUrl;
    private String errorMessage;
} 