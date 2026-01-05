package com.ticket.system.service;

import com.ticket.system.dto.request.PaymentDTO;
import com.ticket.system.dto.response.PaymentResultDTO;

public interface PaymentService {
    PaymentResultDTO createPayment(PaymentDTO paymentDTO);
    PaymentResultDTO getPaymentByOrderId(Long orderId);
    PaymentResultDTO getPaymentByNumber(String paymentNumber);
    boolean processPaymentCallback(String paymentNumber, String status);
    boolean refundPayment(Long orderId);
    void updatePaymentStatus(Long paymentId, Integer status);
}