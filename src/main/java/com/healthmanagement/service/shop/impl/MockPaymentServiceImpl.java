package com.healthmanagement.service.shop.impl;

import com.healthmanagement.dao.shop.OrderDAO;
import com.healthmanagement.dto.shop.PaymentRequest;
import com.healthmanagement.dto.shop.PaymentResponse;
import com.healthmanagement.model.shop.Order;
import com.healthmanagement.service.shop.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class MockPaymentServiceImpl implements PaymentService {

    private final Map<String, PaymentResponse> paymentStore = new HashMap<>();

    @Autowired
    private OrderDAO orderDAO;

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        // 檢查訂單狀態
        Order order = orderDAO.findById(Integer.parseInt(request.getOrderId()));
        if (order == null) {
            PaymentResponse errorResponse = new PaymentResponse();
            errorResponse.setStatus("ERROR");
            errorResponse.setErrorMessage("Order not found");
            return errorResponse;
        }

        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(UUID.randomUUID().toString());
        response.setOrderId(request.getOrderId());
        response.setAmount(request.getAmount());
        response.setCurrency(request.getCurrency());
        
        // 根據訂單狀態設置支付狀態
        if ("completed".equalsIgnoreCase(order.getStatus())) {
            response.setStatus("COMPLETED");
            response.setErrorMessage("Payment already completed");
        } else {
            response.setStatus("PENDING");
            response.setPaymentUrl("http://localhost:8080/api/order/payment/mock/" + response.getPaymentId());
        }
        
        paymentStore.put(response.getPaymentId(), response);
        return response;
    }

    @Override
    public PaymentResponse queryPaymentStatus(String paymentId) {
        PaymentResponse response = paymentStore.get(paymentId);
        if (response == null) {
            response = new PaymentResponse();
            response.setStatus("NOT_FOUND");
            response.setErrorMessage("Payment not found");
        }
        return response;
    }

    @Override
    @Transactional
    public boolean handlePaymentCallback(String callbackData) {
        // 模擬支付回調處理
        String paymentId = callbackData;
        PaymentResponse response = paymentStore.get(paymentId);
        if (response != null) {
            response.setStatus("COMPLETED");
            
            // 更新訂單狀態
            Order order = orderDAO.findById(Integer.parseInt(response.getOrderId()));
            if (order != null) {
                order.setStatus("completed");
                orderDAO.save(order);
            }
            
            return true;
        }
        return false;
    }
} 