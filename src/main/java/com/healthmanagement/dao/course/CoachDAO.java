package com.healthmanagement.dao.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.healthmanagement.model.course.Coach;

@Repository
public interface CoachDAO extends JpaRepository<Coach, Integer> {
    // 你可以加入自訂查詢方法，例如 findByName(String name)
}
