package com.healthmanagement.dao.shop;

import com.healthmanagement.model.shop.Order;
import java.util.List;

public interface OrderDAO {
    Order save(Order order);
    Order findById(Integer id);
    List<Order> findByUserId(Integer userId);
}