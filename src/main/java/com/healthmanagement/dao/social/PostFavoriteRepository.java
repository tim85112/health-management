package com.healthmanagement.dao.social;

import com.healthmanagement.model.social.PostFavorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PostFavoriteRepository extends JpaRepository<PostFavorite, Integer> {
    boolean existsByUserIdAndPostId(Integer userId, Integer postId);
    void deleteByUserIdAndPostId(Integer userId, Integer postId);
    List<PostFavorite> findByUserId(Integer userId);
    
    Optional<PostFavorite> findByUserIdAndPostId(Integer userId, Integer postId);
}