package com.healthmanagement.controller.social;

import com.healthmanagement.dto.social.*;
import com.healthmanagement.service.social.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    @Autowired
    private AnalyticsService analyticsService;

    @GetMapping("/posts/monthly")
    public MonthlyStatDTO getMonthlyPostCount() {
        return analyticsService.getMonthlyPostStats();
    }

    @GetMapping("/comments/monthly")
    public MonthlyStatDTO getMonthlyCommentCount() {
        return analyticsService.getMonthlyCommentStats();
    }

    @GetMapping("/posts/top-liked")
    public RankingStatDTO getTopLikedPosts() {
        return analyticsService.getTopLikedPosts();
    }

    @GetMapping("/posts/top-favorited")
    public RankingStatDTO getTopFavoritedPosts() {
        return analyticsService.getTopFavoritedPosts();
    }

    @GetMapping("/users/top-posts")
    public RankingStatDTO getTopPostUsers() {
        return analyticsService.getTopActiveUsers();
    }

    @GetMapping("/users/top-friends")
    public RankingStatDTO getTopFriendsUsers() {
        return analyticsService.getTopFriendUsers();
    }

    @GetMapping("/training-invitations")
    public TrainingStatDTO getTrainingStats() {
        return analyticsService.getTrainingInvitationStats();
    }
}