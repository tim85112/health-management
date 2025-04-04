package com.healthmanagement.dao.shop;

import com.healthmanagement.entity.shop.CartItem;
import java.util.List;

public interface CartItemDAO {
    CartItem save(CartItem cartItem);
    void delete(Integer id);
    CartItem findById(Integer id);
    List<CartItem> findByUserId(Integer userId);
    CartItem findByUserIdAndProductId(Integer userId, Integer productId);
    void deleteByUserId(Integer userId);
} 