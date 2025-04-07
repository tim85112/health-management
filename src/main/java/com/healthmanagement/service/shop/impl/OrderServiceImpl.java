package com.healthmanagement.service.shop.impl;

import com.healthmanagement.dao.shop.OrderDAO;
import com.healthmanagement.dao.shop.CartItemDAO;
import com.healthmanagement.dao.shop.ProductDAO;
import com.healthmanagement.dto.shop.CreateOrderRequest;
import com.healthmanagement.dto.shop.OrderDTO;
import com.healthmanagement.dto.shop.OrderItemDTO;
import com.healthmanagement.entity.shop.Order;
import com.healthmanagement.entity.shop.OrderItem;
import com.healthmanagement.entity.shop.CartItem;
import com.healthmanagement.entity.shop.Product;
import com.healthmanagement.service.shop.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderDAO orderDAO;

    @Autowired
    private CartItemDAO cartItemDAO;

    @Autowired
    private ProductDAO productDAO;

    @Override
    @Transactional
    public OrderDTO createOrder(CreateOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("訂單項目不能為空");
        }

        // 1. 檢查所有商品是否在購物車中且數量足夠
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            CartItem cartItem = cartItemDAO.findByUserIdAndProductId(request.getUserId(), itemRequest.getProductId());
            if (cartItem == null) {
                throw new RuntimeException("商品 ID:" + itemRequest.getProductId() + " 不在購物車中");
            }
            if (cartItem.getQuantity() < itemRequest.getQuantity()) {
                throw new RuntimeException("商品 ID:" + itemRequest.getProductId() + " 購物車中數量不足，購物車數量: " + 
                    cartItem.getQuantity() + "，請求數量: " + itemRequest.getQuantity());
            }
        }

        // 2. 創建訂單
        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setStatus("pending");
        order.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        order.setOrderItems(new ArrayList<>());
        
        // 3. 創建訂單項目並計算總金額
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (CreateOrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productDAO.findById(itemRequest.getProductId());
            if (product == null) {
                throw new RuntimeException("商品不存在: " + itemRequest.getProductId());
            }
            
            // 檢查庫存
            if (product.getStockQuantity() < itemRequest.getQuantity()) {
                throw new RuntimeException("商品 " + product.getName() + " 庫存不足");
            }

            // 創建訂單項目
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setSubtotal(product.getPrice().multiply(new BigDecimal(itemRequest.getQuantity())));
            
            // 添加到訂單的項目列表中
            order.getOrderItems().add(orderItem);

            // 更新總金額
            totalAmount = totalAmount.add(orderItem.getSubtotal());

            // 更新庫存
            product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
            productDAO.save(product);

            // 從購物車中移除該商品
            CartItem cartItem = cartItemDAO.findByUserIdAndProductId(request.getUserId(), product.getId());
            if (cartItem != null) {
                cartItemDAO.delete(cartItem.getId());
            }
        }

        order.setTotalAmount(totalAmount);
        order = orderDAO.save(order);

        // 4. 返回訂單DTO
        return convertToDTO(order);
    }

    @Override
    @Transactional
    public OrderDTO createOrderFromCart(Integer userId) {
        // 1. 獲取購物車項目
        List<CartItem> cartItems = cartItemDAO.findByUserId(userId);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("購物車為空");
        }

        // 2. 創建訂單
        Order order = new Order();
        order.setUserId(userId);
        order.setStatus("pending");
        order.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        order.setTotalAmount(BigDecimal.ZERO);
        order.setOrderItems(new ArrayList<>());

        // 3. 創建訂單項目並計算總金額
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();
            
            // 檢查庫存
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new RuntimeException("商品 " + product.getName() + " 庫存不足");
            }

            // 創建訂單項目
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(product.getPrice().multiply(new BigDecimal(cartItem.getQuantity())));
            
            // 添加到訂單的項目列表中
            order.getOrderItems().add(orderItem);

            // 更新總金額
            totalAmount = totalAmount.add(orderItem.getSubtotal());

            // 更新庫存
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productDAO.save(product);
        }

        order.setTotalAmount(totalAmount);
        order = orderDAO.save(order);

        // 4. 清空購物車
        cartItemDAO.deleteByUserId(userId);

        // 5. 返回訂單DTO
        return convertToDTO(order);
    }

    @Override
    public OrderDTO getOrderById(Integer orderId) {
        Order order = orderDAO.findById(orderId);
        if (order == null) {
            throw new RuntimeException("訂單不存在");
        }
        return convertToDTO(order);
    }

    @Override
    public List<OrderDTO> getOrdersByUserId(Integer userId) {
        List<Order> orders = orderDAO.findByUserId(userId);
        return orders.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt().toLocalDateTime());

        List<OrderItemDTO> orderItemDTOs = order.getOrderItems().stream()
                .map(this::convertToOrderItemDTO)
                .collect(Collectors.toList());
        dto.setOrderItems(orderItemDTOs);

        return dto;
    }

    private OrderItemDTO convertToOrderItemDTO(OrderItem orderItem) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(orderItem.getId());
        dto.setOrderId(orderItem.getOrder().getId());
        
        if (orderItem.getProduct() != null) {
            dto.setProductId(orderItem.getProduct().getId());
            dto.setProductName(orderItem.getProduct().getName());
            dto.setProductPrice(orderItem.getProduct().getPrice());
        }
        
        dto.setQuantity(orderItem.getQuantity());
        dto.setSubtotal(orderItem.getSubtotal());

        return dto;
    }
} 