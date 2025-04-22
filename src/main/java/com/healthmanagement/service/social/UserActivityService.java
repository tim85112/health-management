package com.healthmanagement.service.social;

import com.healthmanagement.model.social.UserActivity;

import java.util.List;

public interface UserActivityService {
    void logActivity(Integer userId, String actionType, Integer referenceId);
    List<UserActivity> getUserActivities(Integer userId);
}
