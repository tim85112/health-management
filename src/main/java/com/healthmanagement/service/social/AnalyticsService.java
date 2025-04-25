package com.healthmanagement.service.social;

import com.healthmanagement.dto.social.*;

public interface AnalyticsService {
    MonthlyStatDTO getMonthlyPostStats();
    MonthlyStatDTO getMonthlyCommentStats();
    RankingStatDTO getTopLikedPosts();
    RankingStatDTO getTopFavoritedPosts();
    RankingStatDTO getTopActiveUsers();
    RankingStatDTO getTopFriendUsers();
    TrainingStatDTO getTrainingInvitationStats();
}
