package com.healthmanagement.model.shop;

import jakarta.persistence.*;
import lombok.Data;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "cart_item")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "course_id")
    private Integer courseId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity = 1;

    @Column(name = "added_at")
    private Timestamp addedAt;
} 