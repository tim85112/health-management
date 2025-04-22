package com.healthmanagement.service.shop;

import com.healthmanagement.dto.shop.CartItemDTO;
import com.healthmanagement.dto.shop.CartItemRequest;
import java.math.BigDecimal;
import java.util.List;

public interface CartItemService {
    List<CartItemDTO> getCartItems(Integer userId);
    CartItemDTO addToCart(CartItemRequest request);
    CartItemDTO updateQuantity(Integer cartItemId, Integer quantity);
    void removeFromCart(Integer cartItemId);
    void clearCart(Integer userId);
    BigDecimal calculateCartTotal(Integer userId);
} 