package com.healthmanagement.dto.shop;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class NewebpayPaymentRequest {
    private BigDecimal amount;
    private String currency = "TWD";
    private String method = "CREDIT_CARD"; // CREDIT_CARD, ATM, CVS, BARCODE
    private String description;
    private String returnUrl;
    private String notifyUrl;
    private String clientBackUrl;
    private String itemDesc;
    private String orderComment;
    private String language = "zh-tw";
} 