package com.healthmanagement.dto.course;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class CourseResponse {
    private Integer id;
    private String name;
    private String description;
    private Integer coachId;
    private String coachName;
    private LocalDate date;
    private Integer duration;
    private Integer maxCapacity;
}
