package com.healthmanagement.dto.course;

import lombok.Builder;
import lombok.Data;
import java.time.LocalTime;
import java.time.LocalDateTime; // 假設 enrollmentTime 是 LocalDateTime



@Data
@Builder
public class EnrollmentDTO {

    private Integer id;
    private Integer userId;
    private Integer courseId;
    private Integer dayOfWeek; // 新增星期幾
    private LocalTime startTime; // 新增開始時間
    private LocalDateTime enrollmentTime; // 报名时间
    private String status;

}