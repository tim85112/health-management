package com.healthmanagement.service.shop;

import com.healthmanagement.dto.shop.OrderDTO;
import java.util.List;

public interface OrderService {
    OrderDTO createOrderFromCart(Integer userId);
    OrderDTO getOrderById(Integer orderId);
    List<OrderDTO> getOrdersByUserId(Integer userId);
}