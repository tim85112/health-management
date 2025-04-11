package com.healthmanagement.controller.shop;

import com.healthmanagement.util.ApiResponse;
import com.healthmanagement.dto.shop.OrderDTO;
import com.healthmanagement.dto.shop.OrderRequest;
import com.healthmanagement.dto.shop.OrderStatisticsDTO;
import com.healthmanagement.service.shop.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request) {
        OrderDTO order = orderService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping("/cart")
    public ResponseEntity<?> createOrderFromCart(@RequestParam Integer userId) {
        OrderDTO order = orderService.createOrderFromCart(userId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderById(@PathVariable Integer id) {
        OrderDTO order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping
    public ResponseEntity<?> getOrders(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate) {
        
        // 根据查询参数调用相应的服务方法
        List<OrderDTO> orders;
        if (userId != null) {
            orders = orderService.getOrdersByUserId(userId);
        } else if (status != null && !status.isEmpty()) {
            orders = orderService.getOrdersByStatus(status);
        } else if (startDate != null && endDate != null) {
            Timestamp startTimestamp = new Timestamp(startDate.getTime());
            Timestamp endTimestamp = new Timestamp(endDate.getTime());
            orders = orderService.getOrdersByDateRange(startTimestamp, endTimestamp);
        } else {
            // 管理员查看所有订单
            orders = orderService.getAllOrders();
        }
        
        return ResponseEntity.ok(ApiResponse.success(orders));
    }
    
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Integer id, @RequestParam String status) {
        OrderDTO updatedOrder = orderService.updateOrderStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(updatedOrder));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancelOrder(@PathVariable Integer id) {
        orderService.cancelOrder(id);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully"));
    }
    
    @GetMapping("/statistics")
    public ResponseEntity<?> getOrderStatistics() {
        OrderStatisticsDTO statistics = orderService.getOrderStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}