package com.healthmanagement.dto.course;

import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;

@Data
@Builder
public class EnrollmentDTO {
    private Integer id;
    private Integer userId;
    private Integer courseId;
    private Integer dayOfWeek; // 新增星期幾
    private LocalTime startTime; // 新增開始時間
    private String status;
}