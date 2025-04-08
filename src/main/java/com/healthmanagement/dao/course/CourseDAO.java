package com.healthmanagement.dao.course;

import com.healthmanagement.model.course.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseDAO extends JpaRepository<Course, Integer> {
	// 用課程名稱查詢
    List<Course> findByNameContainingIgnoreCase(String name);
    // 用教練ID查詢
    List<Course> findByCoachId(Integer coachId);
    // 用教練名稱查詢
    List<Course> findByCoachNameContainingIgnoreCase(String coachName);
}