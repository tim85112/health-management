package com.healthmanagement.dto.course;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class CourseDTO {
    private Integer id;
    private String name;
    private String description;
    private LocalDate date;
    private Integer coachId;
    private Integer duration;
    private Integer maxCapacity;
}