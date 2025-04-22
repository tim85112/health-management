package com.healthmanagement.dto.social;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserActivityResponse {
    private String actionType;
    private Integer referenceId;
    private LocalDateTime createdAt;

    // 加值欄位（只對 invite 顯示）
    private String message;
    private Integer receiverId;
    
 // for post
    private String postTitle;
    private String postCategory;
}
