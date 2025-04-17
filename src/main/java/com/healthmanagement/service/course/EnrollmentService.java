package com.healthmanagement.service.course;

import java.util.List;

import com.healthmanagement.dto.course.EnrollmentDTO;

public interface EnrollmentService {
    EnrollmentDTO enrollUserToCourse(Integer userId, Integer courseId);
    void cancelEnrollment(Integer enrollmentId);
    EnrollmentDTO getEnrollmentById(Integer enrollmentId);
    List<EnrollmentDTO> getEnrollmentsByUserId(Integer userId);
    List<EnrollmentDTO> getEnrollmentsByCourseId(Integer courseId);
    boolean isCourseFull(Integer courseId);
    boolean isUserEnrolled(Integer userId, Integer courseId);
    int getEnrolledCount(Integer courseId);
}