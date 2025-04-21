package com.healthmanagement.dto.course;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * API 錯誤回應的標準格式。
 * 用於包裝錯誤訊息並返回給客戶端。
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {
	/**
	     * The error message.
	     * 錯誤訊息的內容。
	     */
    private String message;
}