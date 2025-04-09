package com.healthmanagement.dao.shop;

import com.healthmanagement.model.shop.Product;
import java.math.BigDecimal;
import java.util.List;

public interface ProductDAO {
    Product findById(Integer id);
    List<Product> findAll();
    Product save(Product product);
    void delete(Integer id);
    List<Product> search(String keyword);
    List<Product> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);
}