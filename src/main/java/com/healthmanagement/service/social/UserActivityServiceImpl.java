package com.healthmanagement.service.social;

import com.healthmanagement.dao.social.UserActivityRepository;
import com.healthmanagement.model.social.UserActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserActivityServiceImpl implements UserActivityService {

    @Autowired
    private UserActivityRepository activityRepo;

    @Override
    public void logActivity(Integer userId, String actionType, Integer referenceId) {
        UserActivity activity = new UserActivity();
        activity.setUserId(userId);
        activity.setActionType(actionType);
        activity.setReferenceId(referenceId);
        activity.setCreatedAt(LocalDateTime.now());
        activityRepo.save(activity);
    }

    @Override
    public List<UserActivity> getUserActivities(Integer userId) {
        return activityRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
