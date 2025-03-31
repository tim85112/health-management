package com.healthmanagement.service.social;

import com.healthmanagement.model.social.Post;

import java.util.List;

public interface ForumService {

    List<Post> getAllPosts();

    Post getPostById(Integer id);

    Post createPost(Post post);

    Post updatePost(Integer id, Post updatedPost);

    void deletePost(Integer id);
}
