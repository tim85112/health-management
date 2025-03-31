package com.healthmanagement.service.social;

import com.healthmanagement.model.social.Comment;

import java.util.List;

public interface CommentService {

    List<Comment> getCommentsByPostId(Integer postId);

    Comment getCommentById(Integer commentId);

    Comment createComment(Comment comment);

    Comment updateComment(Integer commentId, Comment updated);

    void deleteComment(Integer commentId);
}
