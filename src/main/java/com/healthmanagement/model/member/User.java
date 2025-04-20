package com.healthmanagement.model.member;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.healthmanagement.model.fitness.Achievements;
import com.healthmanagement.model.fitness.ExerciseRecord;
import com.healthmanagement.model.fitness.FitnessGoal;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "[users]")
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

    @Column(name = "gender", columnDefinition = "CHAR(1)")
    private String gender;

    @Column(name = "bio", columnDefinition = "NVARCHAR(MAX)")
    private String bio;

    @Column(name = "role", nullable = false, length = 10)
    private String role;

    @Column(name = "user_points", nullable = false)
    private Integer userPoints;
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("user-exerciseRecords")
    private List<ExerciseRecord> exerciseRecords;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("user-fitnessGoals")
    private List<FitnessGoal> fitnessGoals;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("user-achievements")
    private List<Achievements> achievements;


    @Column(name = "last_login")
    private LocalDateTime lastLogin;
    
    @Column(name = "consecutive_login_days")
    private Integer consecutiveLoginDays; // 新增連續登入天數字段
}