package com.healthmanagement.dao.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.healthmanagement.model.course.Course;
import com.healthmanagement.model.course.Enrollment;
import com.healthmanagement.model.member.User;

import java.util.List;

@Repository
public interface EnrollmentDAO extends JpaRepository<Enrollment, Integer> {
    List<Enrollment> findByCourse(Course course);
    List<Enrollment> findByUser(User user);
    List<Enrollment> findByCourseAndStatus(Course course, String status);
    boolean existsByUserAndCourse(User user, Course course);
    int countByCourseAndStatus(Course course, String status);
}