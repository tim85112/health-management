package com.healthmanagement.controller.social;

import com.healthmanagement.service.member.UserService;
import com.healthmanagement.service.social.MediaService;
import com.healthmanagement.util.FileUploadUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/social/profile")
@Tag(name = "個人社群檔案", description = "個人社群檔案API")
public class SocialProfileController {

    @Autowired
    private UserService userService;

    @Autowired
    private MediaService mediaService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Operation(summary = "上傳使用者大頭貼")
    @PutMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> uploadAvatar(
        @Parameter(description = "大頭貼圖片") 
        @RequestPart("file") MultipartFile file
    ) throws IOException {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Integer userId = userService.findByEmail(email).orElseThrow().getUserId();

        String filename = FileUploadUtil.saveFile(file, uploadDir);
        String imageUrl = "/images/" + filename;

        mediaService.save(imageUrl, "avatar", userId);

        return ResponseEntity.ok(imageUrl);
    }
}
