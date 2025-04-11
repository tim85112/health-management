package com.healthmanagement.dao.shop;

import com.healthmanagement.model.member.User;
import com.healthmanagement.model.shop.CartItem;
import com.healthmanagement.model.shop.Product;

import java.util.List;
import java.util.Optional;

public interface CustomCartItemDAO {
    CartItem save(CartItem cartItem);
    
    void delete(Integer id);
    
    void deleteById(Integer id);
    
    Optional<CartItem> findById(Integer id);
    
    List<CartItem> findByUserId(Integer userId);
    
    CartItem findByUserIdAndProductId(Integer userId, Integer productId);
    
    void deleteByUserId(Integer userId);
    
    // 从JPA接口兼容的方法
    List<CartItem> findByUser(User user);
    
    Optional<CartItem> findByUserAndProduct(User user, Product product);
    
    void deleteAllByUser(User user);
} 