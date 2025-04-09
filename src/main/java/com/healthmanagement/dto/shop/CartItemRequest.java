package com.healthmanagement.dto.shop;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemRequest {
    @NotNull(message = "商品ID不能為空")
    private Integer productId;

    @Min(value = 1, message = "數量必須大於0")
    private Integer quantity = 1;
} 