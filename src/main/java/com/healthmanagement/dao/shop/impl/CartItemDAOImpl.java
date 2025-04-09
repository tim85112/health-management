package com.healthmanagement.dao.shop.impl;

import com.healthmanagement.dao.shop.CartItemDAO;
import com.healthmanagement.model.shop.CartItem;
import com.healthmanagement.model.shop.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class CartItemDAOImpl implements CartItemDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final CartItemRowMapper cartItemRowMapper = new CartItemRowMapper();

    @Override
    public CartItem save(CartItem cartItem) {
        if (cartItem.getId() == null) {
            String sql = "INSERT INTO cart_item (user_id, product_id, quantity, added_at) " +
                        "VALUES (:userId, :productId, :quantity, CURRENT_TIMESTAMP)";
            
            MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", cartItem.getUserId())
                .addValue("productId", cartItem.getProduct().getId())
                .addValue("quantity", cartItem.getQuantity());

            KeyHolder keyHolder = new GeneratedKeyHolder();
            namedParameterJdbcTemplate.update(sql, params, keyHolder);
            cartItem.setId(keyHolder.getKey().intValue());
        } else {
            String sql = "UPDATE cart_item SET user_id = :userId, product_id = :productId, " +
                        "quantity = :quantity, added_at = CURRENT_TIMESTAMP WHERE id = :id";
            
            MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", cartItem.getId())
                .addValue("userId", cartItem.getUserId())
                .addValue("productId", cartItem.getProduct().getId())
                .addValue("quantity", cartItem.getQuantity());

            namedParameterJdbcTemplate.update(sql, params);
        }
        return findById(cartItem.getId());
    }

    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM cart_item WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public CartItem findById(Integer id) {
        String sql = "SELECT ci.id, ci.user_id, ci.quantity, ci.added_at, " +
                    "p.id as product_id, p.name, p.description, p.price, " +
                    "p.stock_quantity, p.image_url, p.created_at, p.updated_at " +
                    "FROM cart_item ci " +
                    "LEFT JOIN product p ON ci.product_id = p.id " +
                    "WHERE ci.id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, cartItemRowMapper, id);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public List<CartItem> findByUserId(Integer userId) {
        String sql = "SELECT ci.id, ci.user_id, ci.quantity, ci.added_at, " +
                    "p.id as product_id, p.name, p.description, p.price, " +
                    "p.stock_quantity, p.image_url, p.created_at, p.updated_at " +
                    "FROM cart_item ci " +
                    "LEFT JOIN product p ON ci.product_id = p.id " +
                    "WHERE ci.user_id = ?";
        return jdbcTemplate.query(sql, cartItemRowMapper, userId);
    }

    @Override
    public CartItem findByUserIdAndProductId(Integer userId, Integer productId) {
        String sql = "SELECT ci.id, ci.user_id, ci.quantity, ci.added_at, " +
                    "p.id as product_id, p.name, p.description, p.price, " +
                    "p.stock_quantity, p.image_url, p.created_at, p.updated_at " +
                    "FROM cart_item ci " +
                    "LEFT JOIN product p ON ci.product_id = p.id " +
                    "WHERE ci.user_id = ? AND ci.product_id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, cartItemRowMapper, userId, productId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void deleteByUserId(Integer userId) {
        String sql = "DELETE FROM cart_item WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }

    private static class CartItemRowMapper implements RowMapper<CartItem> {
        @Override
        public CartItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            CartItem cartItem = new CartItem();
            cartItem.setId(rs.getInt("id"));
            cartItem.setUserId(rs.getInt("user_id"));
            cartItem.setQuantity(rs.getInt("quantity"));
            cartItem.setAddedAt(rs.getTimestamp("added_at"));

            Product product = new Product();
            product.setId(rs.getInt("product_id"));
            product.setName(rs.getString("name"));
            product.setDescription(rs.getString("description"));
            product.setPrice(rs.getBigDecimal("price"));
            product.setStockQuantity(rs.getInt("stock_quantity"));
            product.setImageUrl(rs.getString("image_url"));
            product.setCreatedAt(rs.getTimestamp("created_at"));
            product.setUpdatedAt(rs.getTimestamp("updated_at"));

            cartItem.setProduct(product);
            return cartItem;
        }
    }
} 