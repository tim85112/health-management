package com.healthmanagement.dao.social;

import com.healthmanagement.model.social.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentDAO extends JpaRepository<Comment, Integer> {

    // 查詢某篇文章底下的留言
    List<Comment> findByPost_Id(Integer postId);


    // 查詢某位使用者的所有留言
    List<Comment> findByUserId(Integer userId);

    
    // 查詢某位使用者的所有留言(透過user_id欄位)
    List<Comment> findByUser_Id(Integer userId);

    @Query("SELECT FORMAT(c.createdAt, 'yyyy-MM') AS month, COUNT(c) " +
            "FROM Comment c GROUP BY FORMAT(c.createdAt, 'yyyy-MM') ORDER BY month")
    List<Object[]> countCommentByMonth();

  
    // 計算某位使用者的留言總數
    long countByUser_Id(Integer userId);
}
