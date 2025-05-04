package com.healthmanagement.service.course;

import org.slf4j.Logger; // 引入 Logger
import org.slf4j.LoggerFactory; // 引入 LoggerFactory
import org.springframework.beans.factory.annotation.Value; // 引入 @Value
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ImageServiceImpl implements ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageServiceImpl.class); // 初始化 Logger

    // 使用 @Value 註解讀取 application.yml 中的配置值來初始化儲存根目錄
    private final Path rootLocation;

    // 構造函數，使用 @Value 注入配置值
    // 請確保 application.yml 中有 app.upload.dir 配置
    public ImageServiceImpl(@Value("${app.upload.dir}") String uploadDir) {
        // 將配置的字串路徑轉換為 Path 物件
        this.rootLocation = Paths.get(uploadDir);
        logger.info("圖片儲存根目錄已配置為: {}", this.rootLocation.toAbsolutePath().normalize());

        // 在 Service 啟動時初始化儲存目錄
        try {
            Files.createDirectories(rootLocation.toAbsolutePath().normalize()); // 確保創建的是絕對路徑的目錄
            logger.info("圖片儲存目錄已初始化或已存在: {}", this.rootLocation.toAbsolutePath().normalize());
        } catch (IOException e) {
            logger.error("無法初始化圖片儲存位置: {}", this.rootLocation.toAbsolutePath().normalize(), e);
            throw new RuntimeException("無法初始化儲存位置", e);
        }
    }

    @Override
    public String storeFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        // 為檔案名添加 UUID，同時也對檔案名進行安全處理
        String baseName = originalFilename != null && originalFilename.contains(".")
                          ? originalFilename.substring(0, originalFilename.lastIndexOf("."))
                          : originalFilename;
        // 清理檔案名中的潛在危險字符 (雖然有 UUID，但作為額外防護)
        String safeBaseName = baseName.replaceAll("[^a-zA-Z0-9.\\-_]", "_"); // 移除不安全的字符
        String uniqueFilename = UUID.randomUUID().toString() + "_" + safeBaseName + fileExtension; // 結合 UUID 和清理後的部分檔案名


        // 將唯一檔案名與根目錄結合，得到目標檔案的完整路徑
        // 這裡使用 resolve 是安全的，它會在 rootLocation 下創建 uniqueFilename
        Path destinationFile = this.rootLocation.resolve(uniqueFilename);

        // ====== 修改安全檢查邏輯 ======
        // 確保最終的目標檔案路徑是以 rootLocation 的絕對正規化路徑開頭的
        // 這是一個更安全的檢查，防止通過 ../ 等手段跳出根目錄
        Path normalizedDestinationFile = destinationFile.toAbsolutePath().normalize();
        Path normalizedRootLocation = this.rootLocation.toAbsolutePath().normalize();

        if (!normalizedDestinationFile.startsWith(normalizedRootLocation)) {
             logger.error("安全檢查失敗：嘗試儲存檔案到根目錄外部。目標路徑: {}, 根目錄: {}",
                          normalizedDestinationFile, normalizedRootLocation);
             throw new IOException("無法儲存檔案到指定路徑外");
        }
        // ====== 安全檢查邏輯結束 ======


        // 將檔案複製到目標位置
        try (var inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            logger.info("檔案 '{}' 已成功儲存到 '{}'", originalFilename, normalizedDestinationFile);
        } catch (IOException e) {
            logger.error("儲存檔案 '{}' 到 '{}' 時發生 I/O 錯誤。", originalFilename, normalizedDestinationFile, e);
            throw new IOException("儲存檔案失敗: " + e.getMessage(), e);
        }


        // 返回檔案的可訪問 URL
        // 請將 "http://your-app-url/uploads/images/" 替換為您的應用程式圖片服務的基礎 URL
        // 這個 URL 應該指向一個可以通過 HTTP 訪問到 this.rootLocation 目錄下檔案的端點
        // 通常，如果 this.rootLocation 指向靜態資源目錄，這個 URL 是基於應用程式的上下文路徑
        // 如果您的前端和後端部署在不同伺服器，或者通過 Nginx 代理靜態檔案，這個 URL 會不同
        // 這裡假設您通過後端服務靜態檔案，並且 /uploads/images 路徑對應到 this.rootLocation
        // 您可能需要額外的配置來服務靜態資源
        String baseUrl = "http://localhost:8080/uploads/images/"; // <<<< **** 請根據您的後端靜態資源服務配置修改這個基礎 URL **** >>>>
        // String baseUrl = "http://您的域名或IP/uploads/images/"; // 範例：如果使用 Nginx 服務靜態檔案
        String fileUrl = baseUrl + uniqueFilename;
        logger.info("生成圖片可訪問 URL: {}", fileUrl);
        return fileUrl;
    }

    @Override
    public void deleteImageByUrl(String imageUrl) throws IOException {
         // 這個方法的實現需要根據您的 URL 結構和檔案儲存方式來寫
         // 範例：從 URL 提取檔案名並刪除本地檔案
         // 您可能需要在 application.yml 中配置這個基礎 URL，以便在這裡讀取
         String baseUrl = "http://localhost:8080/uploads/images/"; // <<<< **** 確保與 storeFile 中使用的 baseUrl 一致 **** >>>>

         if (imageUrl != null && imageUrl.startsWith(baseUrl)) {
              String filename = imageUrl.substring(baseUrl.length());
              // 安全檢查：確保要刪除的檔案路徑在根目錄下
              Path filePath = this.rootLocation.resolve(filename);
              Path normalizedFilePath = filePath.toAbsolutePath().normalize();
              Path normalizedRootLocation = this.rootLocation.toAbsolutePath().normalize();

              if (!normalizedFilePath.startsWith(normalizedRootLocation) || normalizedFilePath.equals(normalizedRootLocation)) {
                   logger.warn("嘗試刪除根目錄外部的檔案或根目錄本身：{}", normalizedFilePath);
                   throw new IOException("無效的圖片 URL，不允許刪除指定路徑外的檔案。");
              }

              try {
                  boolean deleted = Files.deleteIfExists(filePath);
                  if (deleted) {
                      logger.info("檔案 '{}' 已成功刪除。", normalizedFilePath);
                  } else {
                      logger.warn("嘗試刪除檔案 '{}' 時，檔案不存在。", normalizedFilePath);
                  }
              } catch (IOException e) {
                   logger.error("刪除檔案失敗: {}", normalizedFilePath, e);
                   throw new IOException("刪除檔案失敗: " + filename, e);
              }
         } else {
              logger.warn("刪除圖片失敗：無效或非應用程式管理的圖片 URL: {}", imageUrl);
              // 這裡可以選擇拋出異常或只是記錄警告，取決於業務需求
              // throw new IllegalArgumentException("無效或非應用程式管理的圖片 URL");
         }
         // 您可能還需要在這裡處理刪除對應的 CourseImage Entity 的邏輯，
         // 或者這個方法只負責刪除檔案，Entity 的刪除在 CourseService 中處理
         // 例如：courseImageRepository.deleteByImageUrl(imageUrl);
    }
}