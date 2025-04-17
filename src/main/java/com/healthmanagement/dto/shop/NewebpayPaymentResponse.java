package com.healthmanagement.dto.shop;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class NewebpayPaymentResponse {
    private String paymentId;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String method;
    private String status;
    private String merchantID;
    private String tradeInfo;
    private String tradeSha;
    private String formHTML;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
} 