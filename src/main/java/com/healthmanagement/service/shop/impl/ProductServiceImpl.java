package com.healthmanagement.service.shop.impl;

import com.healthmanagement.dao.shop.ProductDAO;
import com.healthmanagement.dto.shop.ProductDTO;
import com.healthmanagement.dto.shop.ProductRequest;
import com.healthmanagement.model.shop.Product;
import com.healthmanagement.service.shop.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductDAO productDAO;

    @Override
    @Transactional
    public ProductDTO createProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        product.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        
        product = productDAO.save(product);
        return convertToDTO(product);
    }

    @Override
    @Transactional
    public ProductDTO updateProduct(Integer id, ProductRequest request) {
        Product product = productDAO.findById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setImageUrl(request.getImageUrl());
        product.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        
        product = productDAO.save(product);
        return convertToDTO(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Integer id) {
        Product product = productDAO.findById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        productDAO.delete(id);
    }

    @Override
    public ProductDTO getProductById(Integer id) {
        Product product = productDAO.findById(id);
        if (product == null) {
            throw new RuntimeException("商品不存在");
        }
        return convertToDTO(product);
    }

    @Override
    public List<ProductDTO> getAllProducts() {
        return productDAO.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> searchProducts(String keyword) {
        return productDAO.search(keyword).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return productDAO.findByPriceRange(minPrice, maxPrice).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setImageUrl(product.getImageUrl());
        dto.setCreatedAt(product.getCreatedAt().toLocalDateTime());
        dto.setUpdatedAt(product.getUpdatedAt().toLocalDateTime());
        return dto;
    }
} 