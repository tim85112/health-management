package com.healthmanagement.service.course;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.healthmanagement.dao.course.CourseDAO;
import com.healthmanagement.dao.course.EnrollmentDAO;
import com.healthmanagement.dao.member.UserDAO;
import com.healthmanagement.dto.course.EnrollmentDTO;
import com.healthmanagement.model.course.Course;
import com.healthmanagement.model.course.Enrollment;
import com.healthmanagement.model.member.User;

import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EnrollmentServiceImpl implements EnrollmentService {

    @Autowired
    private EnrollmentDAO enrollmentDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private CourseDAO courseDAO;

    private static final String REGISTERED_STATUS = "已報名";
    private static final String CANCELLED_STATUS = "已取消";
    private static final String WAITING_STATUS = "候補中";

    @Override
    public EnrollmentDTO enrollUserToCourse(Integer userId, Integer courseId) {
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));

        if (isCourseFull(courseId)) {
            throw new IllegalStateException("課程已額滿");
        }
        if (isUserEnrolled(userId, courseId)) {
            throw new IllegalStateException("您已報名此課程");
        }

        Enrollment enrollment = Enrollment.builder()
                .user(user)
                .course(course)
                .enrollmentTime(LocalDateTime.now())
                .status(REGISTERED_STATUS)
                .build();
        Enrollment savedEnrollment = enrollmentDAO.save(enrollment);
        return convertToDTO(savedEnrollment);
    }

    @Override
    public void cancelEnrollment(Integer enrollmentId) {
        Enrollment enrollment = enrollmentDAO.findById(enrollmentId)
                .orElseThrow(() -> new EntityNotFoundException("找不到報名 ID: " + enrollmentId));
        enrollment.setStatus(CANCELLED_STATUS);
        enrollmentDAO.save(enrollment);
    }

    @Override
    public EnrollmentDTO getEnrollmentById(Integer enrollmentId) {
        Enrollment enrollment = enrollmentDAO.findById(enrollmentId)
                .orElseThrow(() -> new EntityNotFoundException("找不到報名 ID: " + enrollmentId));
        return convertToDTO(enrollment);
    }

    @Override
    public List<EnrollmentDTO> getEnrollmentsByUserId(Integer userId) {
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        return enrollmentDAO.findByUser(user).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<EnrollmentDTO> getEnrollmentsByCourseId(Integer courseId) {
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));
        return enrollmentDAO.findByCourse(course).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isCourseFull(Integer courseId) {
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));
        int enrolledCount = enrollmentDAO.countByCourseAndStatus(course, REGISTERED_STATUS);
        return enrolledCount >= course.getMaxCapacity();
    }

    @Override
    public boolean isUserEnrolled(Integer userId, Integer courseId) {
        User user = userDAO.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));
        return enrollmentDAO.existsByUserAndCourse(user, course);
    }

    @Override
    public int getEnrolledCount(Integer courseId) {
        Course course = courseDAO.findById(courseId)
                .orElseThrow(() -> new EntityNotFoundException("Course not found with id: " + courseId));
        return enrollmentDAO.countByCourseAndStatus(course, REGISTERED_STATUS);
    }

    private EnrollmentDTO convertToDTO(Enrollment enrollment) {
        return EnrollmentDTO.builder()
                .id(enrollment.getId())
                .userId(enrollment.getUser().getId())
                .courseId(enrollment.getCourse().getId())
                .enrollmentTime(enrollment.getEnrollmentTime())
                .status(enrollment.getStatus())
                .build();
    }
}