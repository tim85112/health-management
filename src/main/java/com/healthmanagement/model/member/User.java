package com.healthmanagement.model.member;

import java.util.List;

import com.healthmanagement.model.fitness.Achievements;
import com.healthmanagement.model.fitness.ExerciseRecord;
import com.healthmanagement.model.fitness.FitnessGoal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "Users")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "gender", length = 1)
    private String gender;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Column(name = "role", nullable = false, length = 10)
    private String role;

    @Column(name = "user_points", columnDefinition = "INT DEFAULT 0")
    private Integer userPoints;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<ExerciseRecord> exerciseRecords; // 健身紀錄
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<FitnessGoal> fitnessGoals; // 健身目標

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Achievements> achievements; // 成就


}