package com.healthmanagement.dto.course; // 確保在正確的套件下

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // 提供 Getter, Setter, toString, equals, hashCode
@Builder // 提供 Builder 模式
@NoArgsConstructor // 提供無參數建構子
@AllArgsConstructor // 提供所有參數建構子
public class CourseImageDTO {
    private Integer id; // 用於更新時識別現有的圖片
    private String imageUrl; // 圖片 URL
    private Integer imageOrder; // 圖片顯示順序
    // 如果 CourseImage Entity 還有其他需要給前端的欄位，可以在這裡添加
}