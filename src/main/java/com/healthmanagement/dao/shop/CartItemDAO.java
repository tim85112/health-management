package com.healthmanagement.dao.shop;

import com.healthmanagement.model.member.User;
import com.healthmanagement.model.shop.CartItem;
import com.healthmanagement.model.shop.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemDAO extends JpaRepository<CartItem, Integer> {
    List<CartItem> findByUser(User user);
    
    Optional<CartItem> findByUserAndProduct(User user, Product product);
    
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.user = :user")
    void deleteAllByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(c) FROM CartItem c WHERE c.user = :user")
    Long countByUser(@Param("user") User user);
    
    @Query("SELECT SUM(c.quantity) FROM CartItem c WHERE c.user = :user")
    Integer getTotalQuantityByUser(@Param("user") User user);
} 