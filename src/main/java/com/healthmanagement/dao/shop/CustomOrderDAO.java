package com.healthmanagement.dao.shop;

import com.healthmanagement.model.member.User;
import com.healthmanagement.model.shop.Order;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface CustomOrderDAO {
    Order save(Order order);
    
    Optional<Order> findById(Integer id);
    
    List<Order> findByUserId(Integer userId);
    
    List<Order> findByStatus(String status);
    
    BigDecimal getTotalRevenue();
    
    // 添加JPA兼容方法
    List<Order> findByUser(User user);
    
    List<Order> findOrdersByDateRange(Timestamp startDate, Timestamp endDate);
    
    List<Order> findAll();
    
    Long count();
} 