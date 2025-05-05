package com.healthmanagement.dto.course; // <<<< 請確保套件路徑正確 >>>>

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {
    private String url;
    // private Integer id; // 如果您的上傳服務能返回圖片 ID，可以在這裡添加
    // 其他需要的欄位
}