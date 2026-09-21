package com.microservices.OrderService.repository;

import com.microservices.OrderService.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;


@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT COUNT(i) > 0 FROM IdempotencyRecord i WHERE i.idempotencyKey = ?1 AND i.expiresAt > ?2")
    boolean existsByIdempotencyKeyAndNotExpired(String idempotencyKey, LocalDateTime now);

    @Modifying
    @Query("DELETE FROM IdempotencyRecord i WHERE i.expiresAt < ?1")
    int deleteExpiredRecords(LocalDateTime now);
}
