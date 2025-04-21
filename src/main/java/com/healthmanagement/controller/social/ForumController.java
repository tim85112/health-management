package com.healthmanagement.controller.social;

import com.healthmanagement.model.social.Post;
import com.healthmanagement.model.member.User;
import com.healthmanagement.service.social.ForumService;
import com.healthmanagement.service.social.PostLikeService;
import com.healthmanagement.service.member.UserService;
import com.healthmanagement.dto.social.PostRequest;
import com.healthmanagement.dto.social.PostResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@Tag(name = "文章管理", description = "文章管理API")
public class ForumController {

    @Autowired
    private ForumService forumService;

    @Autowired
    private UserService userService;
    
    @Autowired
    private PostLikeService postLikeService;
    
    @Value("${app.upload.dir}")
    private String uploadDir;

    @GetMapping
    @Operation(summary = "查詢全部")
    public ResponseEntity<List<PostResponse>> getAllPosts() {
        return ResponseEntity.ok(forumService.getAllPostResponses());
    }

    @GetMapping("/{id}")
    @Operation(summary = "使用ID搜尋並增加瀏覽數")
    public ResponseEntity<Post> getPostById(@PathVariable Integer id) {
        return ResponseEntity.ok(forumService.incrementViewCountAndGetPostById(id));
    }

    @Operation(summary = "發表文章")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
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

        Post saved = forumService.createPost(post);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "編輯文章")
    public ResponseEntity<Post> updatePost(@PathVariable Integer id, @RequestBody PostRequest updatedPost) {
        return ResponseEntity.ok(forumService.updatePost(id, updatedPost));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "刪除文章")
    public ResponseEntity<Void> deletePost(@PathVariable Integer id) {
        forumService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{postId}/like")
    @Operation(summary = "按讚")
    public ResponseEntity<String> likePost(@PathVariable Integer postId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByEmail(email).orElseThrow();

        Post post = forumService.getPostById(postId);
        boolean liked = postLikeService.likePost(user, post);
        return liked ? ResponseEntity.ok("Liked") : ResponseEntity.badRequest().body("Already liked");
    }

    @DeleteMapping("/{postId}/like")
    @Operation(summary = "取消讚")
    public ResponseEntity<String> unlikePost(@PathVariable Integer postId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByEmail(email).orElseThrow();

        Post post = forumService.getPostById(postId);
        boolean unliked = postLikeService.unlikePost(user, post);
        return unliked ? ResponseEntity.ok("Unliked") : ResponseEntity.badRequest().body("Not liked yet");
    }
    
    @GetMapping("/user")
    @Operation(summary = "取得目前登入使用者的所有貼文")
    public ResponseEntity<List<PostResponse>> getCurrentUserPosts() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        List<PostResponse> userPosts = forumService.getPostsByUser(user);
        return ResponseEntity.ok(userPosts);
    }
}
