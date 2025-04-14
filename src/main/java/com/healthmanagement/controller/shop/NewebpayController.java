package com.healthmanagement.controller.shop;

import com.healthmanagement.dto.shop.NewebpayPaymentRequest;
import com.healthmanagement.dto.shop.NewebpayPaymentResponse;
import com.healthmanagement.service.shop.NewebpayPaymentService;
import com.healthmanagement.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/newebpay")
public class NewebpayController {

    @Autowired
    private NewebpayPaymentService newebpayPaymentService;

    /**
     * 為訂單創建藍新金流支付
     * @param orderId 訂單ID
     * @param request 支付請求
     * @return 支付響應
     */
    @PostMapping("/orders/{orderId}/payment")
    public ResponseEntity<?> createPayment(
            @PathVariable Integer orderId,
            @RequestBody NewebpayPaymentRequest request) {
        NewebpayPaymentResponse response = newebpayPaymentService.createPayment(orderId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 查詢藍新金流支付狀態
     * @param orderId 訂單ID
     * @return 支付狀態
     */
    @GetMapping("/orders/{orderId}/payment/status")
    public ResponseEntity<?> getPaymentStatus(@PathVariable Integer orderId) {
        NewebpayPaymentResponse response = newebpayPaymentService.getPaymentStatus(orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 模擬藍新金流支付回調
     * @param orderId 訂單ID
     * @param status 支付狀態
     * @return 處理結果
     */
    @PostMapping("/orders/{orderId}/mock")
    public ResponseEntity<?> mockCallback(
            @PathVariable Integer orderId,
            @RequestParam(required = false, defaultValue = "SUCCESS") String status) {
        boolean result = newebpayPaymentService.mockCallback(orderId, status);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 處理藍新金流支付回調
     * @param merchantID 商店代號
     * @param tradeInfo 交易資訊
     * @param tradeSha 交易驗證碼
     * @param version 版本
     * @return 處理結果
     */
    @PostMapping("/callback")
    public ResponseEntity<?> handleCallback(
            @RequestParam String MerchantID,
            @RequestParam String TradeInfo,
            @RequestParam String TradeSha,
            @RequestParam String Version) {
        boolean result = newebpayPaymentService.handleCallback(TradeInfo, TradeSha, Version);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
} 