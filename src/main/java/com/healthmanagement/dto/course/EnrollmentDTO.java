package com.healthmanagement.dto.course;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class EnrollmentDTO {
    private Integer id;
    private Integer userId;
    private Integer courseId;
    private LocalDateTime enrollmentTime;
    private String status;
}