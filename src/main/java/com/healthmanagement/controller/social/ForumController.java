package com.healthmanagement.controller.social;

import com.healthmanagement.util.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/forums")
@Tag(name = "Forums", description = "Forum management APIs")
public class ForumController {

    @GetMapping
    @Operation(summary = "Get all forum posts", description = "Retrieve a list of all forum posts")
    public ResponseEntity<ApiResponse<String>> getAllForumPosts() {
        // 此方法将由组员实现
        return ResponseEntity.ok(ApiResponse.success("Forum posts will be implemented by Team Member D"));
    }
}