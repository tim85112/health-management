package com.healthmanagement.service.shop.impl;

import com.healthmanagement.dao.shop.CustomProductDAO;
import com.healthmanagement.dao.shop.ProductDAO;
import com.healthmanagement.dto.shop.ProductDTO;
import com.healthmanagement.dto.shop.ProductRequest;
import com.healthmanagement.exception.ResourceNotFoundException;
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
    
    @Autowired
    private CustomProductDAO customProductDAO;

    @Override
    @Transactional
    public ProductDTO createProduct(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        product.setCreatedAt(Timestamp.valueOf(LocalDateTime.now()));
        product.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        
        product = productDAO.save(product);
        return convertToDTO(product);
    }

    @Override
    @Transactional
    public ProductDTO updateProduct(Integer id, ProductRequest request) {
        Product product = productDAO.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        product.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));
        
        product = productDAO.save(product);
        return convertToDTO(product);
    }

    @Override
    @Transactional
    public void deleteProduct(Integer id) {
        if (!productDAO.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productDAO.deleteById(id);
    }

    @Override
    public ProductDTO getProductById(Integer id) {
        Product product = productDAO.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
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
        return productDAO.findByNameContainingIgnoreCase(keyword).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        List<Product> products = productDAO.findByPriceBetween(minPrice, maxPrice);
        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> getProductsByCategory(String category) {
        List<Product> products = productDAO.findByCategoryContainingIgnoreCase(category);
        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> getInStockProducts() {
        List<Product> products = productDAO.findInStockProducts();
        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductDTO> getLatestProducts() {
        List<Product> products = productDAO.findLatestProducts();
        return products.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean updateProductStock(Integer productId, Integer quantity) {
        Product product = productDAO.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        if (product.getStockQuantity() < quantity) {
            return false; // 库存不足
        }
        
        product.setStockQuantity(product.getStockQuantity() - quantity);
        productDAO.save(product);
        return true;
    }

    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setCategory(product.getCategory());
        dto.setImageUrl(product.getImageUrl());
        dto.setCreatedAt(product.getCreatedAt().toLocalDateTime());
        dto.setUpdatedAt(product.getUpdatedAt().toLocalDateTime());
        return dto;
    }
} 