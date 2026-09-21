package com.microservices.OrderService.service;

import com.microservices.OrderService.entity.IdempotencyRecord;
import com.microservices.OrderService.exception.DuplicateRequestException;
import com.microservices.OrderService.repository.IdempotencyRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for handling idempotency key validation
 * Practice: Duplicate request protection
 * 
 * Prevents duplicate order creation by tracking idempotency keys
 */
@Slf4j
@Service
public class IdempotencyService {

    private final IdempotencyRecordRepository idempotencyRecordRepository;

    public IdempotencyService(IdempotencyRecordRepository idempotencyRecordRepository) {
        this.idempotencyRecordRepository = idempotencyRecordRepository;
    }

    /**
     * Check if idempotency key already exists
     * If exists and not expired, throw DuplicateRequestException
     * 
     * @param idempotencyKey Unique key from request header
     * @throws DuplicateRequestException if key already exists
     */
    @Transactional(readOnly = true)
    public void checkDuplicateRequest(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return; // Idempotency key is optional
        }

        Optional<IdempotencyRecord> existing = idempotencyRecordRepository
                .findByIdempotencyKey(idempotencyKey);

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();
            
            // Check if not expired
            if (record.getExpiresAt().isAfter(LocalDateTime.now())) {
                log.warn("Duplicate request detected: idempotencyKey={}, existingOrderId={}", 
                         idempotencyKey, record.getOrderId());
                
                throw new DuplicateRequestException(
                    "Duplicate request detected. Order already created with this idempotency key.",
                    record.getOrderId()
                );
            } else {
                log.info("Idempotency key expired, allowing reuse: {}", idempotencyKey);
            }
        }
    }

    /**
     * Save idempotency record after successful order creation
     * 
     * @param idempotencyKey Unique key from request
     * @param orderId Created order ID
     * @param orderStatus Order status
     */
    @Transactional
    public void saveIdempotencyRecord(String idempotencyKey, Long orderId, String orderStatus) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return; // Idempotency key is optional
        }

        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(idempotencyKey);
        record.setOrderId(orderId);
        record.setOrderStatus(orderStatus);

        idempotencyRecordRepository.save(record);
        log.info("Saved idempotency record: idempotencyKey={}, orderId={}", idempotencyKey, orderId);
    }

    /**
     * Cleanup expired idempotency records
     * Runs daily at midnight
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanupExpiredRecords() {
        log.info("Starting cleanup of expired idempotency records");
        int deletedCount = idempotencyRecordRepository.deleteExpiredRecords(LocalDateTime.now());
        log.info("Cleanup completed: {} expired records deleted", deletedCount);
    }
}
