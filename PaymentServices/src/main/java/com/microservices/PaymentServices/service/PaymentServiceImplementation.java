package com.microservices.PaymentServices.service;

import com.microservices.PaymentServices.client.UserServiceClient;
import com.microservices.PaymentServices.dto.PaymentRequest;
import com.microservices.PaymentServices.dto.PaymentResponse;
import com.microservices.PaymentServices.entity.Payment;
import com.microservices.PaymentServices.enums.PaymentStatus;
import com.microservices.PaymentServices.exception.PaymentNotFoundException;
import com.microservices.PaymentServices.repository.PaymentRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PaymentServiceImplementation implements PaymentService{

    private final PaymentRepository paymentRepository;
    private final UserServiceClient userServiceClient;

    public PaymentServiceImplementation(PaymentRepository paymentRepository, UserServiceClient userServiceClient) {
        this.paymentRepository = paymentRepository;
        this.userServiceClient = userServiceClient;
    }

    @Override
    public PaymentResponse processPayment(PaymentRequest request) {
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setPaymentStatus(PaymentStatus.PENDING);

        payment = paymentRepository.save(payment);

        boolean deducted = false;
        try {
            deducted = userServiceClient.deductBalance(request.getUserId(), request.getAmount());
        } catch (Exception e) {
            // Log the error but don't throw - we need to update payment status to FAILED
            System.err.println("Failed to deduct balance: " + e.getMessage());
        }

        payment.setPaymentStatus(deducted ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        payment = paymentRepository.save(payment);

        return toResponse(payment);
    }

    @Override
    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment with id " + id + " not found"));
        return toResponse(payment);
    }

    @Override
    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getPaymentStatus(),
                payment.getCreatedAt()
        );
    }
}
