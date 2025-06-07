package com.example.demo3.dao;

import com.example.demo3.enums.OrderStatus;
import com.example.demo3.model.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {

    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(String userId);

    List<OrderEntity> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    Optional<OrderEntity> findByOrderIdAndUserId(String orderId, String userId);

    @Query("SELECT o FROM OrderEntity o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate")
    List<OrderEntity> findOrdersByDateRange(@Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    @Modifying
    @Query("UPDATE OrderEntity o SET o.status = :status, o.updatedAt = CURRENT_TIMESTAMP WHERE o.orderId = :orderId")
    int updateOrderStatus(@Param("orderId") String orderId, @Param("status") OrderStatus status);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.userId = :userId AND o.status = :status")
    long countByUserIdAndStatus(@Param("userId") String userId, @Param("status") OrderStatus status);

    boolean existsByOrderIdAndStatus(String orderId, OrderStatus status);
}
