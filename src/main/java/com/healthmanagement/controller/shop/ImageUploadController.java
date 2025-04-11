package com.healthmanagement.controller.shop;

import com.healthmanagement.util.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
public class ImageUploadController {
    
    private static final Logger logger = LoggerFactory.getLogger(ImageUploadController.class);

    @Value("${app.upload.dir}")
    private String uploadDir;

    /**
     * 上傳圖片
     * @param file 圖片文件
     * @return 圖片URL
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 獲取當前工作目錄
            String currentDir = System.getProperty("user.dir");
            logger.info("當前工作目錄: {}", currentDir);
            
            // 解析相對路徑
            Path uploadPath = Paths.get(currentDir).resolve(uploadDir).normalize();
            logger.info("完整上傳目錄: {}", uploadPath.toAbsolutePath());
            
            // 創建目錄（如果不存在）
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("已創建目錄: {}", uploadPath);
            }

            // 生成唯一文件名
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;

            // 保存文件
            Path targetPath = uploadPath.resolve(fileName);
            logger.info("完整文件保存路徑: {}", targetPath.toAbsolutePath());
            
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            // 確認文件是否存在
            boolean fileExists = Files.exists(targetPath);
            logger.info("文件保存後是否存在: {}", fileExists);

            // 返回可訪問的URL路徑（從public目錄）
            String imageUrl = "/images/" + fileName;
            
            return ResponseEntity.ok(ApiResponse.success(imageUrl));
        } catch (IOException e) {
            logger.error("圖片上傳失敗", e);
            return ResponseEntity.badRequest().body(ApiResponse.error("圖片上傳失敗: " + e.getMessage()));
        }
    }
} 