package com.healthmanagement.controller.social;

import com.healthmanagement.dto.social.CommentRequest;
import com.healthmanagement.model.social.Comment;
import com.healthmanagement.service.social.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
@Tag(name = "Comments", description = "Forum comment APIs")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping("/post/{postId}")
    @Operation(summary = "Create a new comment under post")
    public ResponseEntity<Comment> createComment(
            @PathVariable Integer postId,
            @RequestBody CommentRequest commentRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(commentService.createComment(postId, userDetails.getUsername(), commentRequest));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a comment")
    public ResponseEntity<Comment> updateComment(
            @PathVariable Integer id,
            @RequestBody CommentRequest commentRequest,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(commentService.updateComment(id, userDetails.getUsername(), commentRequest));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get comment by ID")
    public ResponseEntity<Comment> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(commentService.getCommentById(id));
    }

    @GetMapping("/post/{postId}")
    @Operation(summary = "Get comments by post ID")
    public ResponseEntity<List<Comment>> getByPost(@PathVariable Integer postId) {
        return ResponseEntity.ok(commentService.getCommentsByPostId(postId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a comment")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {
        commentService.deleteComment(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
