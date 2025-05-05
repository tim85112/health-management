package com.healthmanagement.service.course; // <<<< 請確保套件路徑正確 >>>>

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import jakarta.persistence.EntityNotFoundException; // 如果您需要處理刪除圖片時找不到的情況

// 將介面名稱從 ImageStorageService 改為 ImageService
public interface ImageService {
    /**
     * 儲存圖片檔案並返回其可訪問 URL。
     * @param file 要儲存的檔案
     * @return 儲存後的圖片 URL
     * @throws IOException 如果檔案處理失敗
     */
    String storeFile(MultipartFile file) throws IOException;

    /**
     * 根據圖片 URL 刪除圖片檔案和資料庫記錄 (用於獨立刪除 API)。
     * 注意：這裡使用 URL 作為參數是為了方便從前端傳遞，
     * 實際實現時您可能需要根據 URL 查找 CourseImage Entity，
     * 或者在 CourseImage Entity 中儲存檔案路徑/名稱來進行刪除。
     * 或者使用圖片 ID 作為參數 (如果您在上傳時能返回 ID)
     * 例如：void deleteImage(Integer imageId) throws EntityNotFoundException, IOException;
     * @param imageUrl 要刪除的圖片 URL
     * @throws EntityNotFoundException 如果找不到對應圖片記錄
     * @throws IOException 如果檔案刪除失敗
     */
    void deleteImageByUrl(String imageUrl) throws EntityNotFoundException, IOException; // 範例方法
}