package com.healthmanagement.dao.shop.impl;

import com.healthmanagement.dao.shop.ProductDAO;
import com.healthmanagement.entity.shop.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public class ProductDAOImpl implements ProductDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Override
    public Product findById(Integer id) {
        String sql = "SELECT * FROM product WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(Product.class), id);
    }

    @Override
    public List<Product> findAll() {
        String sql = "SELECT * FROM product";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Product.class));
    }

    @Override
    public Product save(Product product) {
        String sql;
        if (product.getId() == null) {
            sql = "INSERT INTO product (name, description, price, stock_quantity, image_url, created_at, updated_at) " +
                  "VALUES (:name, :description, :price, :stockQuantity, :imageUrl, :createdAt, :updatedAt)";
        } else {
            sql = "UPDATE product SET name = :name, description = :description, price = :price, " +
                  "stock_quantity = :stockQuantity, image_url = :imageUrl, updated_at = :updatedAt " +
                  "WHERE id = :id";
        }

        SqlParameterSource paramSource = new BeanPropertySqlParameterSource(product);
        KeyHolder keyHolder = new GeneratedKeyHolder();

        namedParameterJdbcTemplate.update(sql, paramSource, keyHolder);

        if (product.getId() == null) {
            product.setId(keyHolder.getKey().intValue());
        }
        return product;
    }

    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM product WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public List<Product> search(String keyword) {
        String sql = "SELECT * FROM product WHERE name LIKE ? OR description LIKE ?";
        String searchPattern = "%" + keyword + "%";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Product.class), searchPattern, searchPattern);
    }

    @Override
    public List<Product> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        String sql = "SELECT * FROM product WHERE price BETWEEN ? AND ?";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(Product.class), minPrice, maxPrice);
    }
} 