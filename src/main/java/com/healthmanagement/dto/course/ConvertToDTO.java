package com.healthmanagement.dto.course;

import com.healthmanagement.model.course.Course;
import com.healthmanagement.model.course.Enrollment;

import org.springframework.stereotype.Component;

@Component
public class ConvertToDTO {

    public EnrollmentDTO convertToEnrollmentDTO(Enrollment enrollment) {

        Course associatedCourse = enrollment.getCourse(); // 先獲取關聯的 Course
        // 從 Course entity 獲取，加入 Null 檢查
        return EnrollmentDTO.builder()
                .id(enrollment.getId())
                .userId(enrollment.getUser() != null ? enrollment.getUser().getId() : null)
                .courseId(associatedCourse != null ? associatedCourse.getId() : null)
                .dayOfWeek(associatedCourse != null ? associatedCourse.getDayOfWeek() : null)
                .startTime(associatedCourse != null ? associatedCourse.getStartTime() : null) 
                .enrollmentTime(enrollment.getEnrollmentTime())
                .courseName(associatedCourse != null ? associatedCourse.getName() : null)
                .status(enrollment.getStatus())
                .build();
    	}
    }