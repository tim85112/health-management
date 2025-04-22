package com.healthmanagement.model.social;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "user_activity")
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "reference_id")
    private Integer referenceId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
