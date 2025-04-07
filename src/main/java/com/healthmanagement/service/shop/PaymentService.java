package com.healthmanagement.service.shop;

import com.healthmanagement.dto.shop.PaymentRequest;
import com.healthmanagement.dto.shop.PaymentResponse;

public interface PaymentService {
    /**
     * 發起支付請求
     * @param request 支付請求
     * @return 支付響應
     */
    PaymentResponse createPayment(PaymentRequest request);

    /**
     * 查詢支付狀態
     * @param paymentId 支付ID
     * @return 支付狀態
     */
    PaymentResponse queryPaymentStatus(String paymentId);

    /**
     * 處理支付回調
     * @param callbackData 回調數據
     * @return 處理結果
     */
    boolean handlePaymentCallback(String callbackData);
} 