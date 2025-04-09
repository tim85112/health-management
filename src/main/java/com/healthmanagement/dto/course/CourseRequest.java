package com.healthmanagement.dto.course;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CourseRequest {
    private String name;
    private String description;
    private LocalDate date;
    private Integer duration;
    private Integer maxCapacity;
    private Integer coachId;
}
