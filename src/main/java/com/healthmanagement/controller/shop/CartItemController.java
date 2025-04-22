package com.healthmanagement.controller.shop;

import com.healthmanagement.util.ApiResponse;
import com.healthmanagement.dto.shop.CartItemDTO;
import com.healthmanagement.dto.shop.CartItemRequest;
import com.healthmanagement.service.shop.CartItemService;
import com.healthmanagement.util.SecurityUtils;
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
    
    @Autowired
    private SecurityUtils securityUtils;

    @PostMapping("/items")
    @Operation(summary = "添加商品到購物車", description = "將商品添加到用戶的購物車中")
    public ResponseEntity<?> addToCart(@RequestBody CartItemRequest request) {
        try {
            // 如果未提供用户ID，使用当前登录用户的ID
            if (request.getUserId() == null) {
                try {
                    request.setUserId(securityUtils.getCurrentUserId());
                } catch (Exception e) {
                    // 如果无法获取当前用户ID并且请求中没有userId
                    return ResponseEntity.badRequest().body(ApiResponse.error("提供的客戶ID參數為空，請提供userId或確保已登錄"));
                }
            }
            
            // 再次检查userId是否为空
            if (request.getUserId() == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("提供的客戶ID參數為空，請提供userId或確保已登錄"));
            }
            
            CartItemDTO cartItem = cartItemService.addToCart(request);
            return ResponseEntity.ok(ApiResponse.success(cartItem));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/items/{id}/quantity")
    @Operation(summary = "更新購物車商品數量", description = "修改購物車中商品的數量")
    public ResponseEntity<?> updateCartItemQuantity(
            @PathVariable Integer id,
            @RequestParam Integer quantity,
            @RequestParam(required = false) Integer userId) {
        try {
            if (userId == null) {
                try {
                    userId = securityUtils.getCurrentUserId();
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("提供的客戶ID參數為空，請提供userId或確保已登錄"));
                }
            }
            
            if (userId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("提供的客戶ID參數為空，請提供userId或確保已登錄"));
            }
            
            CartItemDTO updatedItem = cartItemService.updateQuantity(id, quantity);
            return ResponseEntity.ok(ApiResponse.success(updatedItem));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/items/{id}")
    @Operation(summary = "從購物車移除商品", description = "從購物車中移除指定商品")
    public ResponseEntity<?> removeFromCart(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer userId) {
        try {
            if (userId == null) {
                try {
                    userId = securityUtils.getCurrentUserId();
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("提供的客戶ID參數為空，請提供userId或確保已登錄"));
                }
            }
            
            if (userId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("提供的客戶ID參數為空，請提供userId或確保已登錄"));
            }
            
            cartItemService.removeFromCart(id);
            return ResponseEntity.ok(ApiResponse.success("Item removed from cart successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/items")
    @Operation(summary = "獲取購物車內容", description = "獲取用戶購物車中的所有商品")
    public ResponseEntity<?> getCartItems(@RequestParam(required = false) Integer userId) {
        try {
            if (userId == null) {
                try {
                    userId = securityUtils.getCurrentUserId();
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("提供的客戶ID參數為空，請提供userId或確保已登錄"));
                }
            }
            
            if (userId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("提供的客戶ID參數為空，請提供userId或確保已登錄"));
            }
            
            List<CartItemDTO> cartItems = cartItemService.getCartItems(userId);
            return ResponseEntity.ok(ApiResponse.success(cartItems));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空購物車", description = "清空用戶購物車中的所有商品")
    public ResponseEntity<?> clearCart(@RequestParam(required = false) Integer userId) {
        try {
            if (userId == null) {
                try {
                    userId = securityUtils.getCurrentUserId();
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("提供的客戶ID參數為空，請提供userId或確保已登錄"));
                }
            }
            
            if (userId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("提供的客戶ID參數為空，請提供userId或確保已登錄"));
            }
            
            cartItemService.clearCart(userId);
            return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/total")
    @Operation(summary = "計算購物車總金額", description = "計算用戶購物車中所有商品的總金額")
    public ResponseEntity<?> calculateCartTotal(@RequestParam(required = false) Integer userId) {
        try {
            if (userId == null) {
                try {
                    userId = securityUtils.getCurrentUserId();
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("提供的客戶ID參數為空，請提供userId或確保已登錄"));
                }
            }
            
            if (userId == null) {
                return ResponseEntity.badRequest().body(ApiResponse.error("提供的客戶ID參數為空，請提供userId或確保已登錄"));
            }
            
            BigDecimal total = cartItemService.calculateCartTotal(userId);
            return ResponseEntity.ok(ApiResponse.success(total));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
} 