package com.healthmanagement.dto.shop;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequest {
    @NotBlank(message = "商品名稱不能為空")
    @Size(max = 255, message = "商品名稱長度不能超過255個字符")
    private String name;

    @Size(max = 1000, message = "商品描述長度不能超過1000個字符")
    private String description;

    @NotNull(message = "價格不能為空")
    @DecimalMin(value = "0.01", message = "價格必須大於0")
    private BigDecimal price;

    @NotNull(message = "庫存不能為空")
    @Min(value = 0, message = "庫存不能為負數")
    private Integer stockQuantity;

    @Size(max = 500, message = "圖片URL長度不能超過500個字符")
    private String imageUrl;
} 