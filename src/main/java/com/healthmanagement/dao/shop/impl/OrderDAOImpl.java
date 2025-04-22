package com.healthmanagement.dao.shop.impl;

import com.healthmanagement.dao.shop.CustomOrderDAO;
import com.healthmanagement.model.member.User;
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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class OrderDAOImpl implements CustomOrderDAO {

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
                .addValue("userId", order.getUser().getId())
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
                .addValue("userId", order.getUser().getId())
                .addValue("totalAmount", order.getTotalAmount())
                .addValue("status", order.getStatus());

            namedParameterJdbcTemplate.update(sql, params);
        }
        return findById(order.getId()).orElse(null);
    }

    @Override
    public Optional<Order> findById(Integer id) {
        String sql = "SELECT o.*, u.name as user_name, u.email as user_email " +
                   "FROM [order] o " +
                   "LEFT JOIN [users] u ON o.user_id = u.user_id " +
                   "WHERE o.id = ?";
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
            return Optional.ofNullable(order);
        } catch (Exception e) {
            System.err.println("查詢訂單詳情時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public List<Order> findByUserId(Integer userId) {
        String sql = "SELECT o.*, u.name as user_name, u.email as user_email " +
                   "FROM [order] o " +
                   "LEFT JOIN [users] u ON o.user_id = u.user_id " +
                   "WHERE o.user_id = ? ORDER BY o.created_at DESC";
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
    
    @Override
    public List<Order> findByStatus(String status) {
        String sql = "SELECT o.*, u.name as user_name, u.email as user_email " +
                   "FROM [order] o " +
                   "LEFT JOIN [users] u ON o.user_id = u.user_id " +
                   "WHERE o.status = ? ORDER BY o.created_at DESC";
        List<Order> orders = jdbcTemplate.query(sql, orderRowMapper, status);
        
        // 为每个订单加载订单项目
        for (Order order : orders) {
            String itemsSql = "SELECT oi.*, p.* FROM order_item oi " +
                            "LEFT JOIN product p ON oi.product_id = p.id " +
                            "WHERE oi.order_id = ?";
            List<OrderItem> items = jdbcTemplate.query(itemsSql, new OrderItemRowMapper(), order.getId());
            order.setOrderItems(items);
        }
        return orders;
    }
    
    @Override
    public BigDecimal getTotalRevenue() {
        String sql = "SELECT SUM(total_amount) FROM [order] WHERE status = 'completed'";
        try {
            return jdbcTemplate.queryForObject(sql, BigDecimal.class);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    
    @Override
    public List<Order> findByUser(User user) {
        return findByUserId(user.getId());
    }
    
    @Override
    public List<Order> findOrdersByDateRange(Timestamp startDate, Timestamp endDate) {
        String sql = "SELECT o.*, u.name as user_name, u.email as user_email " +
                   "FROM [order] o " +
                   "LEFT JOIN [users] u ON o.user_id = u.user_id " +
                   "WHERE o.created_at BETWEEN ? AND ? ORDER BY o.created_at DESC";
        List<Order> orders = jdbcTemplate.query(sql, orderRowMapper, startDate, endDate);
        
        // 为每个订单加载订单项目
        for (Order order : orders) {
            String itemsSql = "SELECT oi.*, p.* FROM order_item oi " +
                            "LEFT JOIN product p ON oi.product_id = p.id " +
                            "WHERE oi.order_id = ?";
            List<OrderItem> items = jdbcTemplate.query(itemsSql, new OrderItemRowMapper(), order.getId());
            order.setOrderItems(items);
        }
        return orders;
    }
    
    @Override
    public List<Order> findAll() {
        String sql = "SELECT o.*, u.name as user_name, u.email as user_email " +
                   "FROM [order] o " +
                   "LEFT JOIN [users] u ON o.user_id = u.user_id " +
                   "ORDER BY o.created_at DESC";
        try {
            List<Order> orders = jdbcTemplate.query(sql, orderRowMapper);
            
            // 为每个订单加载订单项目
            for (Order order : orders) {
                String itemsSql = "SELECT oi.*, p.* FROM order_item oi " +
                                "LEFT JOIN product p ON oi.product_id = p.id " +
                                "WHERE oi.order_id = ?";
                List<OrderItem> items = jdbcTemplate.query(itemsSql, new OrderItemRowMapper(), order.getId());
                order.setOrderItems(items);
            }
            return orders;
        } catch (Exception e) {
            System.err.println("查詢所有訂單時發生錯誤: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    @Override
    public Long count() {
        String sql = "SELECT COUNT(*) FROM [order]";
        return jdbcTemplate.queryForObject(sql, Long.class);
    }

    private static class OrderRowMapper implements RowMapper<Order> {
        @Override
        public Order mapRow(ResultSet rs, int rowNum) throws SQLException {
            Order order = new Order();
            order.setId(rs.getInt("id"));
            
            // 创建User对象并设置完整信息
            com.healthmanagement.model.member.User user = new com.healthmanagement.model.member.User();
            user.setId(rs.getInt("user_id"));
            
            try {
                // 嘗試獲取用戶名稱和郵箱
                String userName = rs.getString("user_name");
                String userEmail = rs.getString("user_email");
                
                user.setName(userName);
                user.setEmail(userEmail);
            } catch (SQLException e) {
                // 如果列不存在，忽略錯誤
            }
            
            order.setUser(user);
            
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