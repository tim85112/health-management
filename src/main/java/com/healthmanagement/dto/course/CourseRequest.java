package com.healthmanagement.dto.course;

import lombok.Data;

import java.time.LocalTime;

@Data
public class CourseRequest {
    private String name;
    private String description;
    private Integer dayOfWeek; // 新增星期幾
    private LocalTime startTime; // 新增開始時間
    private Integer duration;
    private Integer maxCapacity;
    private Integer coachId;
}