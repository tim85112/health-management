package com.healthmanagement.controller.course; // 請根據您的套件結構調整


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.healthmanagement.dto.course.ImageUploadResponse;
import com.healthmanagement.service.course.ImageService;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException; // 如果使用了獨立刪除 API 中的異常

@RestController
@RequestMapping("/api/upload") // 您可以根據需要調整這個基礎路徑
@CrossOrigin(origins = "*") // 允許 Vue 前端的跨域請求
// 將類別名稱從 ImageUploadController 改為 ImageController
public class CourseImageController {

    private static final Logger logger = LoggerFactory.getLogger(CourseImageController.class); // 更新 Logger 名稱

    // 注入 ImageService
    private final ImageService imageService;

    @Autowired
    public CourseImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    /**
     * 處理單個圖片上傳
     * @param file 要上傳的圖片檔案
     * @return 包含圖片 URL 和其他信息的響應
     */
    @PostMapping("/image") // 這個路徑對應前端 ElUpload 的 :action
    public ResponseEntity<?> uploadSingleImage(@RequestParam("file") MultipartFile file) {
        logger.info("收到圖片上傳請求：檔案名稱 - {}", file.getOriginalFilename());

        // 基本檔案類型和大小驗證 (可根據需要擴展)
        if (file.isEmpty()) {
            logger.warn("上傳檔案為空。");
            return ResponseEntity.badRequest().body("上傳檔案不能為空。");
        }

        // 您可能需要更嚴格的檔案類型檢查
        if (!Arrays.asList("image/jpeg", "image/png", "image/gif").contains(file.getContentType())) {
             logger.warn("無效的檔案類型：{}", file.getContentType());
             return ResponseEntity.badRequest().body("只允許上傳 JPG, PNG 或 GIF 格式的圖片。");
        }

        try {
            // ====== 呼叫 ImageService 來處理檔案儲存 ======
            String imageUrl = imageService.storeFile(file);
            // ====== 檔案儲存邏輯結束 ======

            logger.info("圖片 '{}' 上傳成功，URL: {}", file.getOriginalFilename(), imageUrl);

            // 返回包含圖片 URL 的響應 DTO
            // ImageUploadResponse DTO 應該包含 imageUrl 欄位
            ImageUploadResponse response = new ImageUploadResponse(imageUrl);
            // 如果您的 ImageService 在儲存時為圖片分配了 ID，也可以在這裡返回
            // response.setImageId(...)

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("圖片上傳失敗：{}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("圖片上傳失敗：" + e.getMessage());
        }
    }

    /**
     * 處理多個圖片上傳 (如果前端一次上傳多個檔案)
     * ElUpload 配置 multiple 時會為每個檔案發送一個單獨的請求，
     * 但有時也會發送包含多個檔案的請求，取決於前端實現。
     * 為了兼容性，可以考慮這個端點。
     * 不過，ElUpload 通常更適合單個檔案的 POST 到指定 action。
     */
    @PostMapping("/images") // 另一個處理多檔案的端點（可選）
    public ResponseEntity<?> uploadMultipleImages(@RequestParam("files") MultipartFile[] files) {
        logger.info("收到多圖片上傳請求：共 {} 個檔案。", files.length);

        if (files.length == 0) {
            return ResponseEntity.badRequest().body("沒有收到上傳檔案。");
        }

        try {
            List<String> imageUrls = Arrays.stream(files)
                .map(file -> {
                     // 在這裡對每個檔案進行驗證和儲存，並返回其 URL
                     if (file.isEmpty()) {
                         logger.warn("多檔案上傳中發現空檔案，忽略。");
                         return null; // 忽略空檔案
                     }
                     // 同樣進行檔案類型和大小驗證
                     if (!Arrays.asList("image/jpeg", "image/png", "image/gif").contains(file.getContentType())) {
                          logger.warn("多檔案上傳中發現無效的檔案類型：{}，忽略。", file.getContentType());
                          return null; // 忽略無效檔案類型
                     }
                     try {
                          return imageService.storeFile(file); // 呼叫 imageService.storeFile
                     } catch (Exception e) {
                          logger.error("儲存檔案 '{}' 時發生錯誤：{}", file.getOriginalFilename(), e.getMessage());
                          return null; // 儲存失敗返回 null
                     }
                })
                .filter(url -> url != null) // 過濾掉儲存失敗或無效的檔案
                .collect(Collectors.toList());

            if (imageUrls.isEmpty()) {
                 return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("所有圖片上傳或處理失敗。");
            }

            // 返回包含圖片 URL 列表的響應
             // 您可能需要創建一個包含 List<String> 的響應 DTO
            return ResponseEntity.ok(imageUrls);

        } catch (Exception e) {
            logger.error("多圖片上傳失敗：{}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("多圖片上傳失敗：" + e.getMessage());
        }
    }


    // ======= 圖片刪除 API (可選，但推薦) =======
    // 如果您實現了獨立的圖片刪除 API
    /*
    @DeleteMapping("/images/{imageId}") // 路徑範例
    public ResponseEntity<?> deleteImage(@PathVariable Integer imageId) {
        logger.info("收到刪除圖片 ID {} 的請求。", imageId);
        try {
            // 呼叫 ImageService 進行刪除
            imageService.deleteImageById(imageId); // 假設 ImageService 有 deleteImageById 方法

            logger.info("圖片 ID {} 刪除成功。", imageId);
            return ResponseEntity.ok().body("圖片刪除成功。");

        } catch (EntityNotFoundException e) {
             logger.warn("嘗試刪除圖片 ID {}，但未找到。", imageId);
             return ResponseEntity.status(HttpStatus.NOT_FOUND).body("找不到圖片 ID: " + imageId);
        } catch (Exception e) {
            logger.error("刪除圖片 ID {} 失敗：{}", imageId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("圖片刪除失敗：" + e.getMessage());
        }
    }
    */

}