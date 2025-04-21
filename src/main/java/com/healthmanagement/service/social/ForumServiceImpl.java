package com.healthmanagement.service.social;

import com.healthmanagement.dao.social.ForumDAO;
import com.healthmanagement.dao.social.PostFavoriteRepository;
import com.healthmanagement.dto.social.PostRequest;
import com.healthmanagement.dto.social.PostResponse;
import com.healthmanagement.model.member.User;
import com.healthmanagement.model.social.Post;
import com.healthmanagement.service.member.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ForumServiceImpl implements ForumService {

    @Autowired
    private ForumDAO forumDAO;
    
    @Autowired
    private UserActivityService userActivityService;
    
    @Autowired
    private CommentService commentService; // ✅ 加入留言統計
    
    @Autowired
    private PostLikeService postLikeService; // ✅ 加入按讚統計

    @Override
    public List<Post> getAllPosts() {
        return forumDAO.findAll();
    }
    @Autowired
    private UserService userService;
    
    @Autowired
    private PostFavoriteRepository postFavoriteRepository;
    
 // ✅ 新增方法：回傳 PostResponse（含留言數、按讚數）
    @Override
    public List<PostResponse> getAllPostResponses() {
        List<Post> posts = forumDAO.findAll();
        List<PostResponse> responses = new ArrayList<>();
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userService.findByEmail(email).orElseThrow();

        for (Post post : posts) {
            PostResponse dto = new PostResponse();
            dto.setId(post.getId());
            dto.setTitle(post.getTitle());
            dto.setContent(post.getContent());
            dto.setCategory(post.getCategory());
            dto.setUser(post.getUser());
            dto.setViewCount(post.getViewCount());
            dto.setCreatedAt(post.getCreatedAt());
            dto.setUpdatedAt(post.getUpdatedAt());
            
            dto.setCommentCount(commentService.countByPost(post));
            dto.setLikeCount(postLikeService.countLikesByPost(post));
            dto.setLiked(postLikeService.hasUserLiked(post, currentUser));
            dto.setFavorited(
            	    postFavoriteRepository.existsByUserIdAndPostId(currentUser.getUserId(), post.getId())
            	);
            responses.add(dto);
        }

        return responses;
    }

    @Override
    public Post getPostById(Integer id) {
        return forumDAO.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + id));
    }
    
    @Override
    public Post createPost(Post post) {
        post.setCreatedAt(LocalDateTime.now());
        post.setUpdatedAt(LocalDateTime.now());
        Post saved = forumDAO.save(post);
        
        // 記錄使用者發文行為
        userActivityService.logActivity(post.getUser().getUserId(), "post", saved.getId());

        return saved;
    }

    @Override
    public Post updatePost(Integer id, PostRequest updatedPost) {
        Post existingPost = getPostById(id);
        existingPost.setTitle(updatedPost.getTitle());
        existingPost.setContent(updatedPost.getContent());
        existingPost.setCategory(updatedPost.getCategory());
        existingPost.setUpdatedAt(LocalDateTime.now());
        return forumDAO.save(existingPost);
    }

    @Override
    public void deletePost(Integer id) {
        forumDAO.deleteById(id);
    }
    
    @Override
    public Post incrementViewCountAndGetPostById(Integer id) {
        Post post = getPostById(id);
        post.setViewCount(post.getViewCount() + 1);  // 累加點擊數
        return forumDAO.save(post); // 儲存更新後資料
    }
    
    @Override
    public List<PostResponse> getPostsByUser(User user) {
        List<Post> posts = forumDAO.findByUser(user); // 這個方法你可以自己加到 ForumDAO 裡
        List<PostResponse> responses = new ArrayList<>();
        for (Post post : posts) {
            PostResponse dto = new PostResponse();
            dto.setId(post.getId());
            dto.setTitle(post.getTitle());
            dto.setContent(post.getContent());
            dto.setCategory(post.getCategory());
            dto.setUser(post.getUser());
            dto.setViewCount(post.getViewCount());
            dto.setCreatedAt(post.getCreatedAt());
            dto.setUpdatedAt(post.getUpdatedAt());
            dto.setCommentCount(commentService.countByPost(post));
            dto.setLikeCount(postLikeService.countLikesByPost(post));
            dto.setLiked(postLikeService.hasUserLiked(post, user));
            responses.add(dto);
        }
        return responses;
    }
}

