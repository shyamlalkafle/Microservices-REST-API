package com.microservices.PaymentServices.service;

import com.microservices.PaymentServices.dto.PaymentRequest;
import com.microservices.PaymentServices.dto.PaymentResponse;

import java.util.List;

public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest request);
    PaymentResponse getPaymentById(Long id);
    List<PaymentResponse> getPaymentsByOrderId(Long orderId);
}
