package com.healthmanagement.dao.social;

import com.healthmanagement.model.social.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentDAO extends JpaRepository<Comment, Integer> {

    // 查詢某篇文章底下的留言
    List<Comment> findByPost_Id(Integer postId);

    // 查詢某位使用者的所有留言（使用 User entity 中的 userId 屬性）
    List<Comment> findByUser_Id(Integer userId);

    // 計算某位使用者的留言總數
    long countByUser_Id(Integer userId);
}
