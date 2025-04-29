package com.healthmanagement.service.social;

import com.healthmanagement.dto.social.*;
import com.healthmanagement.dao.social.CommentDAO;
import com.healthmanagement.dao.social.ForumDAO;
import com.healthmanagement.dao.social.FriendRepository;
import com.healthmanagement.dao.social.PostFavoriteRepository;
import com.healthmanagement.dao.social.PostLikeRepository;
import com.healthmanagement.dao.social.TrainingInvitationRepository;

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
    
    @Autowired
    private CommentDAO commentDAO;

    @Autowired
    private PostFavoriteRepository postFavoriteRepository;
    
    @Autowired
    private FriendRepository friendRepository;

    @Autowired
    private TrainingInvitationRepository trainingInvitationRepository;

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
        List<Object[]> result = commentDAO.countCommentByMonth();
        List<String> months = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Object[] row : result) {
            months.add((String) row[0]);
            counts.add(((Number) row[1]).intValue());
        }
        return new MonthlyStatDTO(months, counts);
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
        List<Object[]> result = postFavoriteRepository.findTopFavoritedPosts();
        List<String> titles = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Object[] row : result) {
            titles.add((String) row[0]);
            counts.add(((Number) row[1]).intValue());
        }
        return new RankingStatDTO(titles, counts);
    }

    @Override
    public RankingStatDTO getTopPostUsers() {
        List<Object[]> result = forumDAO.countPostsByUser();
        List<String> names = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        int limit = Math.min(5, result.size());
        for (int i = 0; i < limit; i++) {
            Object[] row = result.get(i);
            names.add((String) row[0]);
            counts.add(((Number) row[1]).intValue());
        }
        return new RankingStatDTO(names, counts);
    }
    
    @Override
    public RankingStatDTO getTopFriendUsers() {
        List<Object[]> result = friendRepository.countFriendsByUser();
        List<String> names = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        int limit = Math.min(5, result.size());
        for (int i = 0; i < limit; i++) {
            Object[] row = result.get(i);
            names.add(row[0].toString()); // 這邊是 userId，要看你要不要再轉成名字
            counts.add(((Number) row[1]).intValue());
        }
        return new RankingStatDTO(names, counts);
    }

    @Override
    public TrainingStatDTO getTrainingInvitationStats() {
        long accepted = trainingInvitationRepository.countByStatus("accepted");
        long rejected = trainingInvitationRepository.countByStatus("rejected");
        long pending = trainingInvitationRepository.countByStatus("pending");
        return new TrainingStatDTO((int) accepted, (int) rejected, (int) pending);
    }
}
