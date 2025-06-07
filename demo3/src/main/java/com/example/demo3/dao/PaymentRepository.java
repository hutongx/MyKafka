package com.example.demo3.dao;

import com.example.demo3.enums.PaymentStatus;
import com.example.demo3.model.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, String> {

    List<PaymentEntity> findByOrderIdOrderByProcessedAtDesc(String orderId);

    List<PaymentEntity> findByStatusOrderByProcessedAtDesc(PaymentStatus status);

    Optional<PaymentEntity> findByTransactionId(String transactionId);

    @Query("SELECT p FROM PaymentEntity p WHERE p.processedAt >= :startDate AND p.processedAt <= :endDate")
    List<PaymentEntity> findPaymentsByDateRange(@Param("startDate") LocalDateTime startDate,
                                                @Param("endDate") LocalDateTime endDate);

    @Modifying
    @Query("UPDATE PaymentEntity p SET p.status = :status, p.updatedAt = CURRENT_TIMESTAMP WHERE p.paymentId = :paymentId")
    int updatePaymentStatus(@Param("paymentId") String paymentId, @Param("status") PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM PaymentEntity p WHERE p.status = 'SUCCESS' AND p.processedAt >= :startDate")
    BigDecimal getTotalSuccessfulPayments(@Param("startDate") LocalDateTime startDate);

    boolean existsByOrderIdAndStatus(String orderId, PaymentStatus status);
}
