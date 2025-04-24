package com.healthmanagement.service.social;

import com.healthmanagement.dto.social.*;
import com.healthmanagement.dao.social.ForumDAO;
import com.healthmanagement.dao.social.PostLikeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    @Autowired
    private ForumDAO forumDAO;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Override
    public MonthlyStatDTO getMonthlyPostStats() {
        List<Object[]> result = forumDAO.countPostByMonth();
        List<String> months = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Object[] row : result) {
            months.add((String) row[0]);
            counts.add(((Number) row[1]).intValue());
        }
        return new MonthlyStatDTO(months, counts);
    }

    @Override
    public MonthlyStatDTO getMonthlyCommentStats() {
        return new MonthlyStatDTO(
                List.of("1月", "2月", "3月", "4月"),
                List.of(34, 47, 50, 42)
        );
    }

    @Override
    public RankingStatDTO getTopLikedPosts() {
        List<Object[]> result = postLikeRepository.findTopLikedPosts();
        List<String> titles = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Object[] row : result) {
            titles.add((String) row[0]);
            counts.add(((Number) row[1]).intValue());
        }
        return new RankingStatDTO(titles, counts);
    }

    @Override
    public RankingStatDTO getTopFavoritedPosts() {
        return new RankingStatDTO(
                List.of("文章X", "文章Y", "文章Z", "文章W", "文章Q"),
                List.of(22, 21, 18, 15, 13)
        );
    }

    @Override
    public RankingStatDTO getTopActiveUsers() {
        return new RankingStatDTO(
                List.of("Amy", "John", "Lisa", "Tom", "Kevin"),
                List.of(20, 18, 16, 14, 12)
        );
    }

    @Override
    public RankingStatDTO getTopFriendUsers() {
        return new RankingStatDTO(
                List.of("Amy", "Kevin", "John", "Sara", "Chris"),
                List.of(10, 9, 9, 8, 7)
        );
    }

    @Override
    public TrainingStatDTO getTrainingInvitationStats() {
        return new TrainingStatDTO(18, 7, 10);
    }
}
