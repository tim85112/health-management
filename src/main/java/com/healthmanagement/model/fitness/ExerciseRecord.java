package com.healthmanagement.model.fitness;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.healthmanagement.model.member.User;

@Entity
@Table(name = "exercise_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExerciseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")  
    private Integer recordId;

    @Column(name = "user_id", nullable = false)  
    private Integer userId;

    @Column(name = "exercise_type", nullable = false, length = 50 , columnDefinition = "NVARCHAR(50)")  
    private String exerciseType;

    @Column(name = "exercise_duration", nullable = false)  
    private Integer exerciseDuration;

    @Column(name = "calories_burned", nullable = false)  
    private Double caloriesBurned;

    @Column(name = "exercise_date", nullable = false)  
    private LocalDate exerciseDate;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    @JsonIgnore
    private User user;
}
