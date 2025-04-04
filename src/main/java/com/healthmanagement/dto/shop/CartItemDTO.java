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
    private Integer courseId;
    private String courseName;
    private BigDecimal coursePrice;
    private Integer quantity;
    private BigDecimal subtotal;
    private LocalDateTime addedAt;
} 