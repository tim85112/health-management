package com.healthmanagement.service.shop.impl;

import com.healthmanagement.dao.shop.CustomOrderDAO;
import com.healthmanagement.dto.shop.NewebpayPaymentRequest;
import com.healthmanagement.dto.shop.NewebpayPaymentResponse;
import com.healthmanagement.model.shop.Order;
import com.healthmanagement.service.shop.NewebpayPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class NewebpayPaymentServiceImpl implements NewebpayPaymentService {

    private final Map<Integer, NewebpayPaymentResponse> paymentStore = new HashMap<>();
    private final String merchantID = "MS12345678"; // 模擬商店代號
    private final String hashKey = "12345678901234567890123456789012"; // 模擬HashKey
    private final String hashIV = "1234567890123456"; // 模擬HashIV

    @Autowired
    private CustomOrderDAO orderDAO;

    @Override
    public NewebpayPaymentResponse createPayment(Integer orderId, NewebpayPaymentRequest request) {
        // 檢查訂單
        Order order = orderDAO.findById(orderId).orElse(null);
        if (order == null) {
            NewebpayPaymentResponse errorResponse = new NewebpayPaymentResponse();
            errorResponse.setStatus("ERROR");
            errorResponse.setErrorMessage("Order not found");
            return errorResponse;
        }

        // 創建支付響應
        NewebpayPaymentResponse response = new NewebpayPaymentResponse();
        response.setPaymentId("NPP" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        response.setOrderId(orderId.toString());
        response.setAmount(request.getAmount() != null ? request.getAmount() : order.getTotalAmount());
        response.setCurrency(request.getCurrency());
        response.setMethod(request.getMethod());
        response.setCreatedAt(LocalDateTime.now());
        response.setStatus("PENDING");
        response.setMerchantID(merchantID);

        // 生成模擬的交易資訊和交易驗證碼
        String tradeInfo = generateTradeInfo(response);
        String tradeSha = generateTradeSha(tradeInfo);
        response.setTradeInfo(tradeInfo);
        response.setTradeSha(tradeSha);

        // 生成模擬的表單HTML
        String formHtml = generatePaymentFormHtml(response);
        response.setFormHTML(formHtml);

        // 保存支付信息
        paymentStore.put(orderId, response);
        return response;
    }

    @Override
    public NewebpayPaymentResponse getPaymentStatus(Integer orderId) {
        NewebpayPaymentResponse response = paymentStore.get(orderId);
        if (response == null) {
            response = new NewebpayPaymentResponse();
            response.setStatus("NOT_FOUND");
            response.setErrorMessage("Payment not found for orderId: " + orderId);
        }
        return response;
    }

    @Override
    @Transactional
    public boolean mockCallback(Integer orderId, String status) {
        NewebpayPaymentResponse response = paymentStore.get(orderId);
        if (response == null) {
            return false;
        }

        if ("SUCCESS".equalsIgnoreCase(status)) {
            response.setStatus("SUCCESS");
            response.setPaidAt(LocalDateTime.now());

            // 更新訂單狀態
            Order order = orderDAO.findById(orderId).orElse(null);
            if (order != null) {
                order.setStatus("completed");
                orderDAO.save(order);
            }

            return true;
        } else if ("FAILED".equalsIgnoreCase(status)) {
            response.setStatus("FAILED");
            response.setErrorMessage("Payment failed");

            // 更新訂單狀態
            Order order = orderDAO.findById(orderId).orElse(null);
            if (order != null) {
                order.setStatus("payment_failed");
                orderDAO.save(order);
            }

            return true;
        }

        return false;
    }

    @Override
    @Transactional
    public boolean handleCallback(String tradeInfo, String tradeSha, String version) {
        // 在實際場景中，這裡要處理回調，但在模擬環境下，我們簡化處理
        // 假設tradeInfo解密後包含orderId
        try {
            // 簡化處理，實際場景中需要進行解密和驗證
            String decodedInfo = new String(Base64.getDecoder().decode(tradeInfo));
            // 假設decodedInfo格式為: "orderId=123&status=SUCCESS"
            String[] pairs = decodedInfo.split("&");
            String orderId = null;
            String status = "FAILED";
            
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    if ("orderId".equals(keyValue[0])) {
                        orderId = keyValue[1];
                    } else if ("status".equals(keyValue[0])) {
                        status = keyValue[1];
                    }
                }
            }
            
            if (orderId != null) {
                return mockCallback(Integer.parseInt(orderId), status);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    // 生成模擬的交易資訊
    private String generateTradeInfo(NewebpayPaymentResponse response) {
        String info = "MerchantID=" + merchantID + 
                      "&RespondType=JSON" + 
                      "&TimeStamp=" + System.currentTimeMillis()/1000 + 
                      "&Version=2.0" + 
                      "&MerchantOrderNo=" + response.getOrderId() + 
                      "&Amt=" + response.getAmount().intValue() + 
                      "&ItemDesc=Order" + response.getOrderId() + 
                      "&TradeLimit=900" + 
                      "&ClientBackURL=http://localhost:8080/backpage/shop/payment-result?orderId=" + response.getOrderId();
        
        // 模擬AES加密和Base64編碼
        return Base64.getEncoder().encodeToString(info.getBytes());
    }
    
    // 生成模擬的交易驗證碼
    private String generateTradeSha(String tradeInfo) {
        // 模擬SHA256 Hash
        String shaStr = "HashKey=" + hashKey + "&" + tradeInfo + "&HashIV=" + hashIV;
        // 簡化處理，實際場景中需要進行SHA256哈希
        // 修正子串長度以避免索引越界
        String uuid = UUID.randomUUID().toString().replace("-", "");
        // 確保長度為64，不足則重複添加
        StringBuilder sb = new StringBuilder(uuid);
        while (sb.length() < 64) {
            sb.append(uuid);
        }
        return sb.substring(0, 64).toUpperCase();
    }
    
    // 生成模擬的支付表單HTML
    private String generatePaymentFormHtml(NewebpayPaymentResponse response) {
        return "<form id='payment-form' method='post' action='http://localhost:8080/api/newebpay/callback'>" +
               "<input type='hidden' name='MerchantID' value='" + response.getMerchantID() + "'>" +
               "<input type='hidden' name='TradeInfo' value='" + response.getTradeInfo() + "'>" +
               "<input type='hidden' name='TradeSha' value='" + response.getTradeSha() + "'>" +
               "<input type='hidden' name='Version' value='2.0'>" +
               "<input type='submit' value='Submit' style='display:none'>" +
               "</form>" +
               "<script>setTimeout(function() { document.getElementById('payment-form').submit(); }, 3000);</script>";
    }
} 