package com.healthmanagement.service.shop.impl;

import com.healthmanagement.dao.shop.CustomOrderDAO;
import com.healthmanagement.dao.shop.CartItemDAO;
import com.healthmanagement.dao.shop.ProductDAO;
import com.healthmanagement.dto.shop.CreateOrderRequest;
import com.healthmanagement.dto.shop.OrderDTO;
import com.healthmanagement.dto.shop.OrderItemDTO;
import com.healthmanagement.dto.shop.OrderRequest;
import com.healthmanagement.dto.shop.OrderStatisticsDTO;
import com.healthmanagement.exception.ResourceNotFoundException;
import com.healthmanagement.model.member.User;
import com.healthmanagement.model.shop.Order;
import com.healthmanagement.model.shop.OrderItem;
import com.healthmanagement.model.shop.CartItem;
import com.healthmanagement.model.shop.Product;
import com.healthmanagement.service.shop.OrderService;
import lombok.RequiredArgsConstructor;
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
    private CustomOrderDAO orderDAO;

    @Autowired
    private CartItemDAO cartItemDAO;

    @Autowired
    private ProductDAO productDAO;

    @Override
    @Transactional
    public OrderDTO createOrder(OrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("訂單項目不能為空");
        }

        // 1. 創建訂單
        Order order = new Order();
        User user = new User();
        user.setId(request.getUserId());
        order.setUser(user);
        order.setStatus("pending");
        order.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        
        // 2. 創建訂單項目並計算總金額
        BigDecimal totalAmount = BigDecimal.ZERO;
        
        for (OrderRequest.OrderItemRequest itemRequest : request.getItems()) {
            Product product = productDAO.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + itemRequest.getProductId()));
            
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
            
            // 添加到訂單
            order.addOrderItem(orderItem);

            // 更新總金額
            totalAmount = totalAmount.add(orderItem.getSubtotal());

            // 更新庫存
            product.setStockQuantity(product.getStockQuantity() - itemRequest.getQuantity());
            productDAO.save(product);
        }

        order.setTotalAmount(totalAmount);
        order = orderDAO.save(order);

        // 3. 返回訂單DTO
        return convertToDTO(order);
    }

    @Override
    @Transactional
    public OrderDTO createOrderFromCart(Integer userId) {
        // 1. 獲取購物車項目
        User user = new User();
        user.setId(userId);
        List<CartItem> cartItems = cartItemDAO.findByUser(user);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("購物車為空");
        }

        // 2. 創建訂單
        Order order = new Order();
        order.setUser(user);
        order.setStatus("pending");
        order.setCreatedAt(new Timestamp(System.currentTimeMillis()));

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
            
            // 添加到訂單
            order.addOrderItem(orderItem);

            // 更新總金額
            totalAmount = totalAmount.add(orderItem.getSubtotal());

            // 更新庫存
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productDAO.save(product);
        }

        order.setTotalAmount(totalAmount);
        order = orderDAO.save(order);

        // 4. 清空購物車
        cartItemDAO.deleteAllByUser(user);

        // 5. 返回訂單DTO
        return convertToDTO(order);
    }

    @Override
    public OrderDTO getOrderById(Integer orderId) {
        try {
            return orderDAO.findById(orderId)
                .map(this::convertToDTO)
                .orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<OrderDTO> getOrdersByUserId(Integer userId) {
        User user = new User();
        user.setId(userId);
        List<Order> orders = orderDAO.findByUser(user);
        return orders.stream()
                .<OrderDTO>map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getOrdersByStatus(String status) {
        List<Order> orders = orderDAO.findByStatus(status);
        return orders.stream()
                .<OrderDTO>map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderDTO> getOrdersByDateRange(Timestamp startDate, Timestamp endDate) {
        List<Order> orders = orderDAO.findOrdersByDateRange(startDate, endDate);
        return orders.stream()
                .<OrderDTO>map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public OrderStatisticsDTO getOrderStatistics() {
        Long totalOrders = orderDAO.count();
        Long completedOrders = Long.valueOf(orderDAO.findByStatus("completed").size());
        Long pendingOrders = Long.valueOf(orderDAO.findByStatus("pending").size());
        Long cancelledOrders = Long.valueOf(orderDAO.findByStatus("cancelled").size());
        BigDecimal totalRevenue = orderDAO.getTotalRevenue();
        
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }
        
        // 计算售出的总产品数量
        Integer totalProductsSold = 0;
        List<Order> completedOrdersList = orderDAO.findByStatus("completed");
        for (Order order : completedOrdersList) {
            for (OrderItem item : order.getOrderItems()) {
                totalProductsSold += item.getQuantity();
            }
        }
        
        return OrderStatisticsDTO.builder()
                .totalOrders(totalOrders)
                .completedOrders(completedOrders)
                .pendingOrders(pendingOrders)
                .cancelledOrders(cancelledOrders)
                .totalRevenue(totalRevenue)
                .totalProductsSold(totalProductsSold)
                .build();
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(Integer id, String status) {
        Order order = orderDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + id));
        
        order.setStatus(status);
        Order updatedOrder = orderDAO.save(order);
        return convertToDTO(updatedOrder);
    }

    @Override
    @Transactional
    public void cancelOrder(Integer orderId) {
        Order order = orderDAO.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        
        // 只有待处理的订单可以取消
        if (!"pending".equals(order.getStatus())) {
            throw new IllegalStateException("Only pending orders can be cancelled");
        }
        
        // 恢复产品库存
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productDAO.save(product);
        }
        
        order.setStatus("cancelled");
        orderDAO.save(order);
    }

    @Override
    public List<OrderDTO> getAllOrders() {
        List<Order> orders = orderDAO.findAll();
        return orders.stream()
                .<OrderDTO>map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private OrderDTO convertToDTO(Order order) {
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());
        
        // 優先使用用戶電子郵箱
        if (order.getUser().getEmail() != null && !order.getUser().getEmail().isEmpty()) {
            dto.setUserName(order.getUser().getEmail());
        } else if (order.getUser().getName() != null && !order.getUser().getName().isEmpty()) {
            dto.setUserName(order.getUser().getName());
        } else {
            // 如果沒有電子郵箱或名稱，使用ID
            dto.setUserName("用戶" + order.getUser().getId());
        }
        
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt().toLocalDateTime());
        
        List<OrderItemDTO> itemDTOs = order.getOrderItems().stream()
                .<OrderItemDTO>map(this::convertToOrderItemDTO)
                .collect(Collectors.toList());
        dto.setOrderItems(itemDTOs);
        
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
            
            // 添加商品圖片URL和類別
            dto.setProductImageUrl(orderItem.getProduct().getImageUrl());
            dto.setProductCategory(orderItem.getProduct().getCategory());
        }
        
        // 确保数量不为空
        dto.setQuantity(orderItem.getQuantity() != null ? orderItem.getQuantity() : 0);
        dto.setSubtotal(orderItem.getSubtotal());

        return dto;
    }
} 