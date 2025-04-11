package com.healthmanagement.dto.shop;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequest {
    @NotBlank(message = "商品名稱不能為空")
    @Size(max = 255, message = "商品名稱不能超過255個字符")
    private String name;

    @Size(max = 1000, message = "商品描述不能超過1000個字符")
    private String description;

    @NotNull(message = "商品價格不能為空")
    @Min(value = 0, message = "商品價格必須大於等於0")
    private BigDecimal price;

    @NotNull(message = "商品庫存不能為空")
    @Min(value = 0, message = "商品庫存必須大於等於0")
    private Integer stockQuantity;

    @Size(max = 100, message = "商品類別不能超過100個字符")
    private String category;

    @Size(max = 500, message = "圖片URL不能超過500個字符")
    private String imageUrl;
} 