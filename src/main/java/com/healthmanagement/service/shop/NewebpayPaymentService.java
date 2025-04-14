package com.healthmanagement.service.shop;

import com.healthmanagement.dto.shop.NewebpayPaymentRequest;
import com.healthmanagement.dto.shop.NewebpayPaymentResponse;

public interface NewebpayPaymentService {
    /**
     * 創建藍新金流支付
     * @param orderId 訂單ID
     * @param request 支付請求
     * @return 支付響應
     */
    NewebpayPaymentResponse createPayment(Integer orderId, NewebpayPaymentRequest request);

    /**
     * 查詢藍新金流支付狀態
     * @param orderId 訂單ID
     * @return 支付狀態
     */
    NewebpayPaymentResponse getPaymentStatus(Integer orderId);

    /**
     * 模擬藍新金流支付回調
     * @param orderId 訂單ID
     * @param status 支付狀態 (SUCCESS/FAILED)
     * @return 處理結果
     */
    boolean mockCallback(Integer orderId, String status);

    /**
     * 處理藍新金流支付回調
     * @param tradeInfo 交易資訊
     * @param tradeSha 交易驗證碼
     * @param version 版本
     * @return 處理結果
     */
    boolean handleCallback(String tradeInfo, String tradeSha, String version);
} 