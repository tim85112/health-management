package com.healthmanagement.service.social;

import com.healthmanagement.dto.social.CommentRequest;
import com.healthmanagement.model.social.Comment;

import java.util.List;

public interface CommentService {

    List<Comment> getCommentsByPostId(Integer postId);

    List<Comment> getCommentsByUserId(Integer userId);

    Comment getCommentById(Integer commentId);

    Comment createComment(Integer postId, String email, CommentRequest request);

    Comment updateComment(Integer commentId, String email, CommentRequest request);

    void deleteComment(Integer commentId, String email);
    
    
}
