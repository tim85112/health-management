package com.healthmanagement.service.shop;

import com.healthmanagement.dto.shop.PaymentRequest;
import com.healthmanagement.dto.shop.PaymentResponse;

public interface PaymentService {
    /**
     * 發起支付請求
     * @param orderId 訂單ID
     * @param request 支付請求
     * @return 支付響應
     */
    PaymentResponse createPayment(Integer orderId, PaymentRequest request);

    /**
     * 查詢支付狀態
     * @param paymentId 支付ID
     * @return 支付狀態
     */
    PaymentResponse getPaymentStatus(String paymentId);

    /**
     * 模擬支付回調
     * @param paymentId 支付ID
     * @param status 支付狀態 (SUCCESS/FAILED)
     * @return 處理結果
     */
    boolean mockPaymentCallback(String paymentId, String status);

    /**
     * 處理支付回調
     * @param paymentId 支付ID
     * @return 處理結果
     */
    boolean handlePaymentCallback(String paymentId);
} 