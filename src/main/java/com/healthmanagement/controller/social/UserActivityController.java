package com.healthmanagement.controller.social;

import com.healthmanagement.dao.social.ForumDAO;
import com.healthmanagement.dao.social.TrainingInvitationRepository;
import com.healthmanagement.dto.social.UserActivityResponse;
import com.healthmanagement.model.social.UserActivity;
import com.healthmanagement.service.member.UserService;
import com.healthmanagement.service.social.UserActivityService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/activity")
@Tag(name = "個人檔案", description = "個人檔案API")
public class UserActivityController {

    @Autowired
    private UserActivityService activityService;

    @Autowired
    private UserService userService;

    private Integer getLoginUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByEmail(email).orElseThrow().getUserId();
    }
    
    @Autowired
    private TrainingInvitationRepository trainingInvitationRepository;
    @Autowired
    private ForumDAO forumDAO;
    @GetMapping("/me")
    @Operation(summary = "個人檔案")
    public ResponseEntity<List<UserActivityResponse>> getMyActivities() {
        Integer userId = getLoginUserId();
        List<UserActivity> raw = activityService.getUserActivities(userId);
        List<UserActivityResponse> result = new ArrayList<>();

        for (UserActivity activity : raw) {
            UserActivityResponse dto = new UserActivityResponse();
            dto.setActionType(activity.getActionType());
            dto.setReferenceId(activity.getReferenceId());
            dto.setCreatedAt(activity.getCreatedAt());

            // enrich if action = invite
            if ("invite".equals(activity.getActionType())) {
                trainingInvitationRepository.findById(activity.getReferenceId()).ifPresent(invite -> {
                    dto.setMessage(invite.getMessage());
                    dto.setReceiverId(invite.getReceiverId());
                });
            }
            
            if ("post".equals(activity.getActionType())) {
                forumDAO.findById(activity.getReferenceId()).ifPresent(post -> {
                    dto.setPostTitle(post.getTitle());
                    dto.setPostCategory(post.getCategory());
                });
            }

            result.add(dto);
        }

        return ResponseEntity.ok(result);
    }
}
