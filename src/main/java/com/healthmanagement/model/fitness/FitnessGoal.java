package com.healthmanagement.model.fitness;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.healthmanagement.model.member.User;

@Entity
@Table(name = "fitness_goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FitnessGoal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goal_id")
    private Integer goalId;

    @Column(name = "goal_type", nullable = false, length = 50 , columnDefinition = "NVARCHAR(50)")
    private String goalType;

    @Column(name = "target_value", nullable = false)
    private Float targetValue;

    @Column(name = "current_progress")
    private Float currentProgress;  // 進度將根據邏輯自動更新

    @Column(name = "unit", length = 20 , columnDefinition = "NVARCHAR(20)")
    private String unit;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date", columnDefinition = "datetime2")
    private LocalDate endDate;

    @Column(name = "status", length = 20 , columnDefinition = "NVARCHAR(50)")
    private String status;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;
}
