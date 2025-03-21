package com.healthmanagement.controller.shop;

import com.healthmanagement.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@Tag(name = "Orders", description = "Order management APIs")
public class OrderController {

    @GetMapping
    @Operation(summary = "Get all orders", description = "Retrieve a list of all orders")
    public ResponseEntity<ApiResponse<String>> getAllOrders() {
        // 此方法将由组员实现
        return ResponseEntity.ok(ApiResponse.success("Order list will be implemented by Team Member A"));
    }
}