package com.healthmanagement.dto.shop;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CartItemDTO {
    private Integer id;
    private Integer userId;
    private Integer productId;
    private String productName;
    private BigDecimal productPrice;
    private String productDescription;
    private Integer productStockQuantity;
    private String productCategory;
    private String productImageUrl;
    private Integer quantity;
    private BigDecimal subtotal;
    private LocalDateTime addedAt;
} 