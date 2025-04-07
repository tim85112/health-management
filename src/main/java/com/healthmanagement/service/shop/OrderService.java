package com.healthmanagement.service.shop;

import com.healthmanagement.dto.shop.CreateOrderRequest;
import com.healthmanagement.dto.shop.OrderDTO;
import java.util.List;

public interface OrderService {
    OrderDTO createOrder(CreateOrderRequest request);
    OrderDTO createOrderFromCart(Integer userId);
    OrderDTO getOrderById(Integer orderId);
    List<OrderDTO> getOrdersByUserId(Integer userId);
}