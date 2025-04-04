package com.healthmanagement.service.shop.impl;

import com.healthmanagement.dao.shop.CartItemDAO;
import com.healthmanagement.dao.shop.ProductDAO;
import com.healthmanagement.dto.shop.CartItemDTO;
import com.healthmanagement.dto.shop.CartItemRequest;
import com.healthmanagement.entity.shop.CartItem;
import com.healthmanagement.entity.shop.Product;
import com.healthmanagement.service.shop.CartItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartItemServiceImpl implements CartItemService {

    @Autowired
    private CartItemDAO cartItemDAO;

    @Autowired
    private ProductDAO productDAO;

    @Override
    @Transactional
    public CartItemDTO addToCart(Integer userId, CartItemRequest request) {
        Product product = productDAO.findById(request.getProductId());
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        CartItem existingItem = cartItemDAO.findByUserIdAndProductId(userId, request.getProductId());
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            existingItem.setAddedAt(Timestamp.valueOf(LocalDateTime.now()));
            cartItemDAO.save(existingItem);
            return convertToDTO(existingItem);
        }

        CartItem cartItem = new CartItem();
        cartItem.setUserId(userId);
        cartItem.setProduct(product);
        cartItem.setQuantity(request.getQuantity());
        cartItem.setAddedAt(Timestamp.valueOf(LocalDateTime.now()));
        cartItem = cartItemDAO.save(cartItem);
        
        return convertToDTO(cartItem);
    }

    @Override
    @Transactional
    public CartItemDTO updateQuantity(Integer userId, Integer cartItemId, Integer quantity) {
        CartItem cartItem = cartItemDAO.findById(cartItemId);
        if (cartItem == null) {
            throw new RuntimeException("購物車項目不存在");
        }
        if (!cartItem.getUserId().equals(userId)) {
            throw new RuntimeException("無權修改此購物車項目");
        }
        if (quantity <= 0) {
            throw new RuntimeException("數量必須大於0");
        }

        cartItem.setQuantity(quantity);
        cartItem.setAddedAt(Timestamp.valueOf(LocalDateTime.now()));
        cartItem = cartItemDAO.save(cartItem);
        return convertToDTO(cartItem);
    }

    @Override
    @Transactional
    public void removeFromCart(Integer userId, Integer cartItemId) {
        CartItem cartItem = cartItemDAO.findById(cartItemId);
        if (cartItem != null && cartItem.getUserId().equals(userId)) {
            cartItemDAO.delete(cartItemId);
        }
    }

    @Override
    public List<CartItemDTO> getCartItems(Integer userId) {
        return cartItemDAO.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void clearCart(Integer userId) {
        cartItemDAO.deleteByUserId(userId);
    }

    @Override
    public BigDecimal calculateTotal(Integer userId) {
        return cartItemDAO.findByUserId(userId).stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CartItemDTO convertToDTO(CartItem cartItem) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(cartItem.getId());
        dto.setUserId(cartItem.getUserId());
        dto.setProductId(cartItem.getProduct().getId());
        dto.setProductName(cartItem.getProduct().getName());
        dto.setProductPrice(cartItem.getProduct().getPrice());
        dto.setQuantity(cartItem.getQuantity());
        dto.setSubtotal(cartItem.getProduct().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        dto.setAddedAt(cartItem.getAddedAt().toLocalDateTime());
        return dto;
    }
} 