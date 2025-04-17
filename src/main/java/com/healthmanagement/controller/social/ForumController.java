package com.healthmanagement.controller.social;

import com.healthmanagement.model.social.Post;
import com.healthmanagement.model.member.User;
import com.healthmanagement.service.social.ForumService;
import com.healthmanagement.service.social.MediaService;
import com.healthmanagement.service.social.PostLikeService;
import com.healthmanagement.util.FileUploadUtil;
import com.healthmanagement.service.member.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthmanagement.dto.social.PostRequest;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;

import java.io.IOException;
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
    
    @Autowired
    private MediaService mediaService;
    
    @Value("${app.upload.dir}")
    private String uploadDir;

    @GetMapping
    @Operation(summary = "查詢全部")
    public ResponseEntity<List<Post>> getAllPosts() {
        return ResponseEntity.ok(forumService.getAllPosts());
    }

    @GetMapping("/{id}")
    @Operation(summary = "使用ID搜尋並增加瀏覽數")
    public ResponseEntity<Post> getPostById(@PathVariable Integer id) {
        return ResponseEntity.ok(forumService.incrementViewCountAndGetPostById(id));
    }

    @Operation(summary = "發表文章")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Post> createPost(
    	@Parameter(
    		        description = "文章資料 JSON 字串，例如：{\"category\":\"fitness\",\"title\":\"範例文章\",\"content\":\"這是內容\"}",
    		        example = "{\"category\":\"fitness\",\"title\":\"範例文章\",\"content\":\"這是內容\"}"
    		    )
        @RequestPart("data") String data,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) throws IOException {
        // 轉換 JSON 成 DTO
        ObjectMapper mapper = new ObjectMapper();
        PostRequest postRequest = mapper.readValue(data, PostRequest.class);

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
        // 儲存圖片（如有）
        if (image != null && !image.isEmpty()) {
            String filename = FileUploadUtil.saveFile(image, uploadDir);
            mediaService.save(filename, "post", post.getId()); // 延後呼叫也可以放在下面
        }

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
}
