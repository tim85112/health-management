package com.healthmanagement.dao.shop.impl;

import com.healthmanagement.dao.shop.OrderDAO;
import com.healthmanagement.model.shop.Order;
import com.healthmanagement.model.shop.OrderItem;
import com.healthmanagement.model.shop.Product;
import org.springframework.beans.factory.annotation.Autowired;
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
public class OrderDAOImpl implements OrderDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final OrderRowMapper orderRowMapper = new OrderRowMapper();

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            String sql = "INSERT INTO [order] (user_id, total_amount, status, created_at) " +
                        "VALUES (:userId, :totalAmount, :status, CURRENT_TIMESTAMP)";
            
            MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", order.getUserId())
                .addValue("totalAmount", order.getTotalAmount())
                .addValue("status", order.getStatus());

            KeyHolder keyHolder = new GeneratedKeyHolder();
            namedParameterJdbcTemplate.update(sql, params, keyHolder);
            order.setId(keyHolder.getKey().intValue());

            // 保存訂單項目
            if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
                for (OrderItem item : order.getOrderItems()) {
                    String itemSql = "INSERT INTO order_item (order_id, product_id, quantity, subtotal) " +
                                   "VALUES (:orderId, :productId, :quantity, :subtotal)";
                    
                    MapSqlParameterSource itemParams = new MapSqlParameterSource()
                        .addValue("orderId", order.getId())
                        .addValue("productId", item.getProduct().getId())
                        .addValue("quantity", item.getQuantity())
                        .addValue("subtotal", item.getSubtotal());
                    
                    namedParameterJdbcTemplate.update(itemSql, itemParams);
                }
            }
        } else {
            String sql = "UPDATE [order] SET user_id = :userId, total_amount = :totalAmount, " +
                        "status = :status WHERE id = :id";
            
            MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", order.getId())
                .addValue("userId", order.getUserId())
                .addValue("totalAmount", order.getTotalAmount())
                .addValue("status", order.getStatus());

            namedParameterJdbcTemplate.update(sql, params);
        }
        return findById(order.getId());
    }

    @Override
    public Order findById(Integer id) {
        String sql = "SELECT * FROM [order] WHERE id = ?";
        Order order;
        try {
            order = jdbcTemplate.queryForObject(sql, orderRowMapper, id);
            if (order != null) {
                // 加載訂單項目
                String itemsSql = "SELECT oi.*, p.* FROM order_item oi " +
                                "LEFT JOIN product p ON oi.product_id = p.id " +
                                "WHERE oi.order_id = ?";
                List<OrderItem> items = jdbcTemplate.query(itemsSql, new OrderItemRowMapper(), id);
                order.setOrderItems(items);
            }
            return order;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<Order> findByUserId(Integer userId) {
        String sql = "SELECT * FROM [order] WHERE user_id = ? ORDER BY created_at DESC";
        List<Order> orders = jdbcTemplate.query(sql, orderRowMapper, userId);
        
        // 為每個訂單加載訂單項目
        for (Order order : orders) {
            String itemsSql = "SELECT oi.*, p.* FROM order_item oi " +
                            "LEFT JOIN product p ON oi.product_id = p.id " +
                            "WHERE oi.order_id = ?";
            List<OrderItem> items = jdbcTemplate.query(itemsSql, new OrderItemRowMapper(), order.getId());
            order.setOrderItems(items);
        }
        return orders;
    }

    private static class OrderRowMapper implements RowMapper<Order> {
        @Override
        public Order mapRow(ResultSet rs, int rowNum) throws SQLException {
            Order order = new Order();
            order.setId(rs.getInt("id"));
            order.setUserId(rs.getInt("user_id"));
            order.setTotalAmount(rs.getBigDecimal("total_amount"));
            order.setStatus(rs.getString("status"));
            order.setCreatedAt(rs.getTimestamp("created_at"));
            return order;
        }
    }

    private static class OrderItemRowMapper implements RowMapper<OrderItem> {
        @Override
        public OrderItem mapRow(ResultSet rs, int rowNum) throws SQLException {
            OrderItem item = new OrderItem();
            item.setId(rs.getInt("id"));
            item.setQuantity(rs.getInt("quantity"));
            item.setSubtotal(rs.getBigDecimal("subtotal"));

            // 創建並設置Product
            Product product = new Product();
            product.setId(rs.getInt("product_id"));
            product.setName(rs.getString("name"));
            product.setDescription(rs.getString("description"));
            product.setPrice(rs.getBigDecimal("price"));
            product.setStockQuantity(rs.getInt("stock_quantity"));
            product.setImageUrl(rs.getString("image_url"));
            product.setCreatedAt(rs.getTimestamp("created_at"));
            product.setUpdatedAt(rs.getTimestamp("updated_at"));
            item.setProduct(product);

            // 創建並設置Order
            Order order = new Order();
            order.setId(rs.getInt("order_id"));
            item.setOrder(order);
            
            return item;
        }
    }
} 