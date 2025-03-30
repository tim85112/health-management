package com.healthmanagement.controller.social;

import com.healthmanagement.model.social.Comment;
import com.healthmanagement.service.social.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
@Tag(name = "Comments", description = "Forum comment APIs")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping("/post/{postId}")
    @Operation(summary = "Get all comments by post ID")
    public ResponseEntity<List<Comment>> getByPost(@PathVariable Integer postId) {
        return ResponseEntity.ok(commentService.getCommentsByPostId(postId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get comment by ID")
    public ResponseEntity<Comment> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(commentService.getCommentById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new comment")
    public ResponseEntity<Comment> create(@RequestBody Comment comment) {
        return ResponseEntity.ok(commentService.createComment(comment));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a comment")
    public ResponseEntity<Comment> update(@PathVariable Integer id, @RequestBody Comment comment) {
        return ResponseEntity.ok(commentService.updateComment(id, comment));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a comment")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        commentService.deleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
