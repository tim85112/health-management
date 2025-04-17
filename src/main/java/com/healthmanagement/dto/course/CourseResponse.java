package com.healthmanagement.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;

@Data
@AllArgsConstructor
public class CourseResponse {
    private Integer id;
    private String name;
    private String description;
    private Integer coachId;
    private String coachName;
    private Integer dayOfWeek; // 新增星期幾
    private LocalTime startTime; // 新增開始時間
    private Integer duration;
    private Integer maxCapacity;
}