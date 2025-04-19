package com.healthmanagement.service.social;

import com.healthmanagement.dto.social.PostRequest;
import com.healthmanagement.dto.social.PostResponse;
import com.healthmanagement.model.member.User;
import com.healthmanagement.model.social.Post;

import java.util.List;

public interface ForumService {
    List<Post> getAllPosts();
    List<PostResponse> getAllPostResponses();
    List<PostResponse> getPostsByUser(User user);
    Post getPostById(Integer id);
    Post incrementViewCountAndGetPostById(Integer id);
    Post createPost(Post post);
    Post updatePost(Integer id, PostRequest updatedPost);
    void deletePost(Integer id);
}
