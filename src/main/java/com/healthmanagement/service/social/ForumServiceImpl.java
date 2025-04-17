package com.healthmanagement.service.social;

import com.healthmanagement.dao.social.ForumDAO;
import com.healthmanagement.dto.social.PostRequest;
import com.healthmanagement.model.social.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ForumServiceImpl implements ForumService {

    @Autowired
    private ForumDAO forumDAO;

    @Override
    public List<Post> getAllPosts() {
        return forumDAO.findAll();
    }

    @Override
    public Post getPostById(Integer id) {
        return forumDAO.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with ID: " + id));
    }

    @Autowired
    private UserActivityService userActivityService;
    
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
}

