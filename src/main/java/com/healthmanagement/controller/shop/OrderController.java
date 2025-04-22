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
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/orders")
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
        try {
            OrderDTO order = orderService.getOrderById(id);
            if (order == null) {
                return ResponseEntity.ok(ApiResponse.error("訂單不存在或已被刪除"));
            }
            return ResponseEntity.ok(ApiResponse.success(order));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error("獲取訂單詳情失敗: " + e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getOrders(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        
        try {
            // 獲取訂單列表
            List<OrderDTO> allOrders;
            if (userId != null) {
                allOrders = orderService.getOrdersByUserId(userId);
            } else if (status != null && !status.isEmpty()) {
                allOrders = orderService.getOrdersByStatus(status);
            } else if (startDate != null && endDate != null) {
                Timestamp startTimestamp = new Timestamp(startDate.getTime());
                Timestamp endTimestamp = new Timestamp(endDate.getTime());
                allOrders = orderService.getOrdersByDateRange(startTimestamp, endTimestamp);
            } else {
                // 管理員查看所有訂單
                allOrders = orderService.getAllOrders();
            }
            
            // 手動分頁處理
            int total = allOrders.size();
            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, total);
            
            List<OrderDTO> pageOrders = fromIndex < total 
                ? allOrders.subList(fromIndex, toIndex) 
                : new ArrayList<>();
            
            // 創建分頁響應格式
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("content", pageOrders);
            responseData.put("totalElements", total);
            responseData.put("totalPages", (int) Math.ceil((double) total / size));
            responseData.put("size", size);
            responseData.put("number", page);
            
            return ResponseEntity.ok(ApiResponse.success(responseData));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error("獲取訂單失敗: " + e.getMessage()));
        }
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