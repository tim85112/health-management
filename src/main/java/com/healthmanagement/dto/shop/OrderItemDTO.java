package com.healthmanagement.dto.shop;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemDTO {
    private Integer id;
    private Integer orderId;
    private Integer productId;
    private String productName;
    private BigDecimal productPrice;
    private String productImageUrl;
    private String productCategory;
    private Integer quantity;
    private BigDecimal subtotal;
} 