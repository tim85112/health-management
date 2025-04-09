package com.healthmanagement.service.shop;

import com.healthmanagement.dto.shop.ProductDTO;
import com.healthmanagement.dto.shop.ProductRequest;
import java.math.BigDecimal;
import java.util.List;

public interface ProductService {
    
    ProductDTO createProduct(ProductRequest request);
    
    ProductDTO updateProduct(Integer id, ProductRequest request);
    
    void deleteProduct(Integer id);
    
    ProductDTO getProductById(Integer id);
    
    List<ProductDTO> getAllProducts();
    
    List<ProductDTO> searchProducts(String keyword);
    
    List<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);
}