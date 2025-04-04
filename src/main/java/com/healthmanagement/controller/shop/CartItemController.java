package com.healthmanagement.controller.shop;

import com.healthmanagement.dto.shop.CartItemDTO;
import com.healthmanagement.dto.shop.CartItemRequest;
import com.healthmanagement.service.shop.CartItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "購物車管理", description = "購物車相關操作")
public class CartItemController {

    @Autowired
    private CartItemService cartItemService;

    @PostMapping("/items")
    @Operation(summary = "添加商品到購物車", description = "將商品添加到用戶的購物車中")
    public ResponseEntity<?> addToCart(
            @Parameter(description = "用戶ID") @RequestParam Integer userId,
            @RequestBody CartItemRequest request) {
        try {
            CartItemDTO result = cartItemService.addToCart(userId, request);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/items/{cartItemId}/quantity")
    @Operation(summary = "更新購物車商品數量", description = "修改購物車中商品的數量")
    public ResponseEntity<?> updateQuantity(
            @Parameter(description = "用戶ID") @RequestParam Integer userId,
            @Parameter(description = "購物車項目ID") @PathVariable Integer cartItemId,
            @Parameter(description = "新的數量") @RequestParam Integer quantity) {
        try {
            CartItemDTO result = cartItemService.updateQuantity(userId, cartItemId, quantity);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", result);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "從購物車移除商品", description = "從購物車中移除指定商品")
    public ResponseEntity<?> removeFromCart(
            @Parameter(description = "用戶ID") @RequestParam Integer userId,
            @Parameter(description = "購物車項目ID") @PathVariable Integer cartItemId) {
        try {
            cartItemService.removeFromCart(userId, cartItemId);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/items")
    @Operation(summary = "獲取購物車內容", description = "獲取用戶購物車中的所有商品")
    public ResponseEntity<?> getCartItems(
            @Parameter(description = "用戶ID") @RequestParam Integer userId) {
        try {
            List<CartItemDTO> items = cartItemService.getCartItems(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", items);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空購物車", description = "清空用戶購物車中的所有商品")
    public ResponseEntity<?> clearCart(
            @Parameter(description = "用戶ID") @RequestParam Integer userId) {
        try {
            cartItemService.clearCart(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/total")
    @Operation(summary = "計算購物車總金額", description = "計算用戶購物車中所有商品的總金額")
    public ResponseEntity<?> calculateTotal(
            @Parameter(description = "用戶ID") @RequestParam Integer userId) {
        try {
            BigDecimal total = cartItemService.calculateTotal(userId);
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("data", total);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 