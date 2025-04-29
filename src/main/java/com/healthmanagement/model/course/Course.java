package com.healthmanagement.model.course;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference; // 引入注解
import com.healthmanagement.model.member.User;

@Data
@Entity
@Table(name = "course") // 資料表名稱
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 設定自動遞增
    @Column(name = "id") // 資料表中的 ID 欄位
    private Integer id;

    @Column(name = "name", nullable = false, length = 255)
    private String name; // 課程名稱

    @Column(name = "description", nullable = false, length = 1000)
    private String description; // 課程描述

    @ManyToOne
    @JoinColumn(name = "coach_id", nullable = false) // 外鍵連接 User 資料表
    @JsonBackReference("user-courses") // 添加名稱與 User 類中相對應
    private User coach;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek; // 課程星期幾 (0: Sunday, 1: Monday, ..., 6: Saturday)

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime; // 課程開始時間，使用 LocalTime 類型

    @Column(name = "duration", nullable = false)
    private Integer duration; // 課程時長（以分鐘為單位）

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity; // 最大容納人數

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
    @JsonManagedReference("course-enrollments")
    private List<Enrollment> enrollments; // 課程的報名紀錄

    @Column(name = "offers_trial_option", nullable = false)
    private Boolean offersTrialOption;

    @Column(name = "max_trial_capacity")
    private Integer maxTrialCapacity;
}