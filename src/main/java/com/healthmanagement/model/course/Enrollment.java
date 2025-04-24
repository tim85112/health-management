package com.healthmanagement.model.course;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.healthmanagement.model.member.User;

@Data
@Entity
@Table(name = "enrollment")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference("user-enrollments")
    private User user; // 報名使用者 (關聯到 User 表格)

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    @JsonBackReference("course-enrollments")
    private Course course; // 報名課程 (關聯到 Course 表格)

    @Builder.Default
    private LocalDateTime enrollmentTime = LocalDateTime.now();

    @Column(nullable = false, length = 50)
    private String status;
}