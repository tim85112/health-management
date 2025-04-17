package com.healthmanagement.service.social;

import com.healthmanagement.model.member.User;
import com.healthmanagement.model.social.Post;
import com.healthmanagement.model.social.PostLike;

import java.util.List;

public interface PostLikeService {
    boolean likePost(User user, Post post);
    boolean unlikePost(User user, Post post);
    boolean isLiked(User user, Post post);
    int countLikes(Post post);
    List<PostLike> getLikesForPost(Post post);
}
