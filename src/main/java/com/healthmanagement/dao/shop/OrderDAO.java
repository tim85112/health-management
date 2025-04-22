package com.healthmanagement.dao.shop;

import com.healthmanagement.model.member.User;
import com.healthmanagement.model.shop.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Repository
public interface OrderDAO extends JpaRepository<Order, Integer> {
    List<Order> findByUser(User user);
    
    List<Order> findByStatus(String status);
    
    @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
    List<Order> findOrdersByDateRange(@Param("startDate") Timestamp startDate, @Param("endDate") Timestamp endDate);
    
    @Query("SELECT o FROM Order o WHERE o.user = :user ORDER BY o.createdAt DESC")
    List<Order> findRecentOrdersByUser(@Param("user") User user);
    
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'completed'")
    BigDecimal getTotalRevenue();
}