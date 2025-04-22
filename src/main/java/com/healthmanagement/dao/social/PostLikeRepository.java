package com.healthmanagement.dao.social;

import com.healthmanagement.model.social.Post;
import com.healthmanagement.model.social.PostLike;
import com.healthmanagement.model.social.PostLikeId;
import com.healthmanagement.model.member.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, PostLikeId> {
    boolean existsByUserAndPost(User user, Post post);
    Optional<PostLike> findByUserAndPost(User user, Post post);
    int countByPost(Post post);
    List<PostLike> findAllByPost(Post post);
    boolean existsByPostAndUser(Post post, User user);
}
