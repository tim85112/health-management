package com.healthmanagement.controller.social;

import com.healthmanagement.model.social.Post;
import com.healthmanagement.model.member.User;
import com.healthmanagement.service.social.ForumService;
import com.healthmanagement.service.member.UserService;
import com.healthmanagement.dto.social.PostRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/posts")
@Tag(name = "Forum Posts", description = "Forum post CRUD APIs")
public class ForumController {

    @Autowired
    private ForumService forumService;

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "Get all forum posts")
    public ResponseEntity<List<Post>> getAllPosts() {
        return ResponseEntity.ok(forumService.getAllPosts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get post by ID")
    public ResponseEntity<Post> getPostById(@PathVariable Integer id) {
        return ResponseEntity.ok(forumService.getPostById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new post")
    public ResponseEntity<Post> createPost(@RequestBody PostRequest postRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userService.findByEmail(email)
                               .orElseThrow(() -> new RuntimeException("User not found"));

        Post post = new Post();
        post.setCategory(postRequest.getCategory());
        post.setTitle(postRequest.getTitle());
        post.setContent(postRequest.getContent());
        post.setUser(user);

        return ResponseEntity.ok(forumService.createPost(post));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing post")
    public ResponseEntity<Post> updatePost(@PathVariable Integer id, @RequestBody PostRequest updatedPost) {
        return ResponseEntity.ok(forumService.updatePost(id, updatedPost));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a post")
    public ResponseEntity<Void> deletePost(@PathVariable Integer id) {
        forumService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}
