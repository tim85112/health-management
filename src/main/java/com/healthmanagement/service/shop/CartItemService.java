package com.healthmanagement.service.shop;

import com.healthmanagement.dto.shop.CartItemDTO;
import com.healthmanagement.dto.shop.CartItemRequest;
import java.math.BigDecimal;
import java.util.List;

public interface CartItemService {
    CartItemDTO addToCart(Integer userId, CartItemRequest request);
    CartItemDTO updateQuantity(Integer userId, Integer cartItemId, Integer quantity);
    void removeFromCart(Integer userId, Integer cartItemId);
    List<CartItemDTO> getCartItems(Integer userId);
    void clearCart(Integer userId);
    BigDecimal calculateTotal(Integer userId);
} 