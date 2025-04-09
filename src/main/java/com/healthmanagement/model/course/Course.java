package com.healthmanagement.model.course;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "course")  // 資料表名稱
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // 設定自動遞增
    @Column(name = "id")  // 資料表中的 ID 欄位
    private Integer id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;  // 課程名稱

    @Column(name = "description", nullable = false, length = 1000)
    private String description;  // 課程描述

    @ManyToOne
    @JoinColumn(name = "coach_id", nullable = false)  // 外鍵連接 coach 資料表
    private Coach coach;

    @Column(name = "date", nullable = false)
    private LocalDate date;  // 課程日期，使用 LocalDate 類型


    @Column(name = "duration", nullable = false)
    private Integer duration;  // 課程時長（以分鐘為單位）

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity;  // 最大容納人數
}
