package com.healthmanagement.service.shop;

import com.healthmanagement.dto.shop.OrderDTO;
import com.healthmanagement.dto.shop.OrderRequest;
import com.healthmanagement.dto.shop.OrderStatisticsDTO;

import java.sql.Timestamp;
import java.util.List;

public interface OrderService {
    
    OrderDTO createOrder(OrderRequest request);
    
    OrderDTO createOrderFromCart(Integer userId);
    
    OrderDTO getOrderById(Integer id);
    
    List<OrderDTO> getOrdersByUserId(Integer userId);
    
    List<OrderDTO> getAllOrders();
    
    OrderDTO updateOrderStatus(Integer id, String status);
    
    List<OrderDTO> getOrdersByStatus(String status);
    
    List<OrderDTO> getOrdersByDateRange(Timestamp startDate, Timestamp endDate);
    
    OrderStatisticsDTO getOrderStatistics();
    
    void cancelOrder(Integer orderId);
}