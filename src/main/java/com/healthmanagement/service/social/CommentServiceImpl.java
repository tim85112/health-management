package com.healthmanagement.service.social;

import com.healthmanagement.dao.social.CommentDAO;
import com.healthmanagement.model.social.Comment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentDAO commentDAO;

    @Override
    public List<Comment> getCommentsByPostId(Integer postId) {
        return commentDAO.findByPost_ArticleId(postId);
    }

    @Override
    public Comment getCommentById(Integer commentId) {
        return commentDAO.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
    }

    @Override
    public Comment createComment(Comment comment) {
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        return commentDAO.save(comment);
    }

    @Override
    public Comment updateComment(Integer commentId, Comment updated) {
        Comment comment = getCommentById(commentId);
        comment.setCommentText(updated.getCommentText());
        comment.setUpdatedAt(LocalDateTime.now());
        return commentDAO.save(comment);
    }

    @Override
    public void deleteComment(Integer commentId) {
        commentDAO.deleteById(commentId);
    }
}
