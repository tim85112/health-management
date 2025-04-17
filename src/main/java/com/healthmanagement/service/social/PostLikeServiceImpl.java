package com.healthmanagement.service.social;

import com.healthmanagement.dao.social.PostLikeRepository;
import com.healthmanagement.model.member.User;
import com.healthmanagement.model.social.Post;
import com.healthmanagement.model.social.PostLike;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostLikeServiceImpl implements PostLikeService {

    @Autowired
    private PostLikeRepository likeRepo;
    
    @Autowired
    private UserActivityService userActivityService;
    
    @Override
    public boolean likePost(User user, Post post) {
        if (likeRepo.existsByUserAndPost(user, post)) return false;

        PostLike like = new PostLike();
        like.setUser(user);
        like.setPost(post);
        like.setCreatedAt(LocalDateTime.now());
        likeRepo.save(like);
        
     // 記錄使用者按讚行為
        userActivityService.logActivity(user.getUserId(), "like", post.getId()); // ✅ 按讚動態
        return true;
    }

    @Override
    public boolean unlikePost(User user, Post post) {
        return likeRepo.findByUserAndPost(user, post).map(like -> {
            likeRepo.delete(like);
            return true;
        }).orElse(false);
    }

    @Override
    public boolean isLiked(User user, Post post) {
        return likeRepo.existsByUserAndPost(user, post);
    }

    @Override
    public int countLikes(Post post) {
        return likeRepo.countByPost(post);
    }

    @Override
    public List<PostLike> getLikesForPost(Post post) {
        return likeRepo.findAllByPost(post);
    }
}
