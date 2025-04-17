package com.healthmanagement.controller.social;

import com.healthmanagement.dao.social.PostFavoriteRepository;
import com.healthmanagement.model.social.PostFavorite;
import com.healthmanagement.service.member.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/favorites")
@Tag(name = "文章收藏", description = "文章收藏API")
public class PostFavoriteController {

    @Autowired
    private PostFavoriteRepository repo;

    @Autowired
    private UserService userService;

    private Integer getLoginUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByEmail(email).orElseThrow().getUserId();
    }

    @Operation(summary = "收藏文章")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "成功收藏文章"),
        @ApiResponse(responseCode = "400", description = "已經收藏過此文章")
    })
    @PostMapping("/{postId}")
    public ResponseEntity<String> addFavorite(@PathVariable Integer postId) {
        Integer userId = getLoginUserId();
        if (repo.existsByUserIdAndPostId(userId, postId)) {
            return ResponseEntity.badRequest().body("已收藏");
        }
        PostFavorite pf = new PostFavorite();
        pf.setUserId(userId);
        pf.setPostId(postId);
        repo.save(pf);
        return ResponseEntity.ok("已收藏");
    }

    @Operation(summary = "取消收藏文章")
    @ApiResponse(responseCode = "200", description = "成功取消收藏")
    @DeleteMapping("/{postId}")
    public ResponseEntity<String> removeFavorite(@PathVariable Integer postId) {
        Integer userId = getLoginUserId();

        Optional<PostFavorite> favorite = repo.findByUserIdAndPostId(userId, postId);
        if (favorite.isEmpty()) {
            return ResponseEntity.badRequest().body("尚未收藏");
        }

        repo.delete(favorite.get());
        return ResponseEntity.ok("已取消收藏");
    }

    @Operation(summary = "取得使用者收藏清單")
    @ApiResponse(responseCode = "200", description = "成功取得收藏清單")
    @GetMapping
    public ResponseEntity<List<PostFavorite>> getMyFavorites() {
        Integer userId = getLoginUserId();
        return ResponseEntity.ok(repo.findByUserId(userId));
    }
}
