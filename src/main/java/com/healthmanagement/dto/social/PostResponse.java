package com.healthmanagement.dto.social;

import com.healthmanagement.model.member.User;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PostResponse {
    private Integer id;
    private String title;
    private String content;
    private String category;
    private User user;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean liked;
    private boolean favorited;

}
