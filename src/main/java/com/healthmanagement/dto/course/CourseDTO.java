package com.healthmanagement.dto.course;

import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;

@Data
@Builder
public class CourseDTO {
    private Integer id;
    private String name;
    private String description;
    private Integer dayOfWeek; // 新增星期幾
    private LocalTime startTime; // 新增開始時間
    private Integer coachId;
    private Integer duration;
    private Integer maxCapacity;
}