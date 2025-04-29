package com.healthmanagement.dto.course;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDTO {

    private Integer id;
    private Integer userId;
    private String userName;
    private Integer courseId;
    private String coachName;
    private LocalDateTime enrollmentTime;
    private String status;
    private Integer dayOfWeek;
    private LocalTime startTime;
    private String courseName;
}