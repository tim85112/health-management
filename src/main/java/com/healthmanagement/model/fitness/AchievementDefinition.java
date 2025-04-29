package com.healthmanagement.model.fitness;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "achievement_definitions")
@Data
public class AchievementDefinition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "definition_id")
    private Integer definitionId;

    @Column(name = "achievement_type", unique = true, nullable = false, length = 50)
    private String achievementType;

    @Column(name = "title", nullable = false, length = 100, columnDefinition = "NVARCHAR")
    private String title;

    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "trigger_event", length = 50)
    private String triggerEvent;

    @Column(name = "trigger_condition", columnDefinition = "NVARCHAR(MAX)")
    private String triggerCondition;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "points")
    private Integer points;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}