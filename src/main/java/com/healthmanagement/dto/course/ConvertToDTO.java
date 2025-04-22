package com.healthmanagement.dto.course;

import com.healthmanagement.model.course.Enrollment;

import org.springframework.stereotype.Component;

@Component
public class ConvertToDTO {

    public EnrollmentDTO convertToDTO(Enrollment enrollment) {
        return EnrollmentDTO.builder()
                .id(enrollment.getId())
                .userId(enrollment.getUser().getId())
                .courseId(enrollment.getCourse().getId())
                .dayOfWeek(enrollment.getCourse().getDayOfWeek()) // 從 Course entity 獲取
                .startTime(enrollment.getCourse().getStartTime()) // 從 Course entity 獲取
                .enrollmentTime(enrollment.getEnrollmentTime())
                .status(enrollment.getStatus())
                .build();
    }
}