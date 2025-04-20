package com.healthmanagement.dao.social;

import com.healthmanagement.model.social.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentDAO extends JpaRepository<Comment, Integer> {

	// 查詢某篇文章底下的留言
	List<Comment> findByPost_Id(Integer postId);

	// 查詢某位使用者的所有留言
	List<Comment> findByUser_UserId(Integer userId);

	// 添加這個方法來計算指定用戶的評論數量
	long  countByUser_UserId(Integer userId);
}
