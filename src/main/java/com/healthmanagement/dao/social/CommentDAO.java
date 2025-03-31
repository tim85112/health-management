package com.healthmanagement.dao.social;

import com.healthmanagement.model.social.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentDAO extends JpaRepository<Comment, Integer> {

    // 查詢某篇文章底下的留言
    List<Comment> findByPost_ArticleId(Integer articleId);

    // 查詢某位使用者的所有留言
    List<Comment> findByUser_UserId(Integer userId);
}

