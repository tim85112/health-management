package com.healthmanagement.dao.social;

import com.healthmanagement.model.social.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserActivityRepository extends JpaRepository<UserActivity, Integer> {
    List<UserActivity> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
