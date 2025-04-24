package com.healthmanagement.model.member;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.healthmanagement.model.course.Course;
import com.healthmanagement.model.course.Enrollment;
import com.healthmanagement.model.course.TrialBooking;
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
    private Integer id;

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

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<ExerciseRecord> exerciseRecords; // 健身紀錄

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<FitnessGoal> fitnessGoals; // 健身目標

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Achievements> achievements; // 成就

    @OneToMany(mappedBy = "coach", cascade = CascadeType.ALL)
    @JsonManagedReference("user-courses")
    private List<Course> coursesCoached; // 課程

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonManagedReference("user-enrollments")
    private List<Enrollment> enrollments; // 報名
    
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    @JsonManagedReference("user-trial-bookings")
    private List<TrialBooking> trialBookings; //預約

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // 兼容性方法
    public Integer getUserId() {
        return this.id;
    }

    public void setUserId(Integer userId) {
        this.id = userId;
    }
}