package com.healthmanagement.service.shop.impl;

import com.healthmanagement.dao.shop.CustomCartItemDAO;
import com.healthmanagement.dao.shop.ProductDAO;
import com.healthmanagement.dto.shop.CartItemDTO;
import com.healthmanagement.dto.shop.CartItemRequest;
import com.healthmanagement.model.member.User;
import com.healthmanagement.model.shop.CartItem;
import com.healthmanagement.model.shop.Product;
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
    private CustomCartItemDAO cartItemDAO;

    @Autowired
    private ProductDAO productDAO;

    @Override
    @Transactional
    public CartItemDTO addToCart(CartItemRequest request) {
        Product product = productDAO.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        User user = new User();
        user.setId(request.getUserId());

        CartItem existingItem = cartItemDAO.findByUserAndProduct(user, product)
                .orElse(null);
                
        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            existingItem.setAddedAt(Timestamp.valueOf(LocalDateTime.now()));
            cartItemDAO.save(existingItem);
            return convertToDTO(existingItem);
        }

        CartItem cartItem = new CartItem();
        cartItem.setUser(user);
        cartItem.setProduct(product);
        cartItem.setQuantity(request.getQuantity());
        cartItem.setAddedAt(Timestamp.valueOf(LocalDateTime.now()));
        cartItem = cartItemDAO.save(cartItem);
        
        return convertToDTO(cartItem);
    }

    @Override
    @Transactional
    public CartItemDTO updateQuantity(Integer cartItemId, Integer quantity) {
        CartItem cartItem = cartItemDAO.findById(cartItemId)
                .orElseThrow(() -> new RuntimeException("購物車項目不存在"));
                
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
    public void removeFromCart(Integer cartItemId) {
        cartItemDAO.deleteById(cartItemId);
    }

    @Override
    public List<CartItemDTO> getCartItems(Integer userId) {
        User user = new User();
        user.setId(userId);
        return cartItemDAO.findByUser(user).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void clearCart(Integer userId) {
        User user = new User();
        user.setId(userId);
        cartItemDAO.deleteAllByUser(user);
    }

    @Override
    public BigDecimal calculateCartTotal(Integer userId) {
        User user = new User();
        user.setId(userId);
        List<CartItem> cartItems = cartItemDAO.findByUser(user);
        
        return cartItems.stream()
                .map(item -> item.getProduct().getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private CartItemDTO convertToDTO(CartItem cartItem) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(cartItem.getId());
        dto.setUserId(cartItem.getUser().getId());
        dto.setProductId(cartItem.getProduct().getId());
        dto.setProductName(cartItem.getProduct().getName());
        dto.setProductPrice(cartItem.getProduct().getPrice());
        dto.setQuantity(cartItem.getQuantity());
        dto.setSubtotal(cartItem.getProduct().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        dto.setAddedAt(cartItem.getAddedAt().toLocalDateTime());
        return dto;
    }
} 