package com.healthmanagement.dao.social;

import com.healthmanagement.model.social.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ForumDAO extends JpaRepository<Post, Integer> {
    
    // 依照分類查文章
    List<Post> findByCategory(String category);
    
    // 依照使用者查文章
    List<Post> findByUserId(Integer userId);
}
