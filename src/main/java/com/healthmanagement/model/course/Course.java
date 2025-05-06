package com.healthmanagement.model.course;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference; // 引入注解
import com.healthmanagement.model.member.User;

@Data
@Entity
@Table(name = "course")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coach_id", nullable = false)
    @JsonBackReference("user-courses")
    private User coach;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;

    @Column(name = "offers_trial_option", nullable = false)
    private Boolean offersTrialOption;

    @Column(name = "max_trial_capacity")
    private Integer maxTrialCapacity;
    
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    @JsonManagedReference("course-enrollments")
    private List<Enrollment> enrollments;
    
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    @JsonManagedReference("course-trialBookings")
    private List<TrialBooking> trialBookings;
    
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference("course-images")
    @Builder.Default
    private List<CourseImage> images = new ArrayList<>();
}