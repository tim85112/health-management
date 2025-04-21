package com.healthmanagement.dao.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.healthmanagement.model.course.Course;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface CourseDAO extends JpaRepository<Course, Integer> {
    // 用課程名稱查詢
    List<Course> findByNameContainingIgnoreCase(String name);
    // 用教練ID查詢 (可以直接使用，因為 coach_id 現在是 users 表格的 user_id)
    List<Course> findByCoachId(Integer coachId);
    // 用教練名稱查詢 (需要使用 @Query 進行關聯查詢)
    @Query("SELECT c FROM Course c JOIN c.coach u WHERE u.name LIKE %:coachName%")
    List<Course> findByCoachNameContainingIgnoreCase(String coachName);
    // 用星期幾查詢
    List<Course> findByDayOfWeek(Integer dayOfWeek);
    // 根據開始時間範圍查詢
    List<Course> findByStartTimeBetween(LocalTime startTime, LocalTime endTime);
}