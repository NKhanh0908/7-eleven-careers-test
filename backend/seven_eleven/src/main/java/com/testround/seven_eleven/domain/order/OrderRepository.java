package com.testround.seven_eleven.domain.order;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query(value = "SELECT nextval('order_daily_seq')", nativeQuery = true)
    Long getNextOrderSequenceValue();

    Page<Order> findByUser_IdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("""
        SELECT o FROM Order o
        WHERE (:orderCode IS NULL OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :orderCode, '%')))
          AND (:status IS NULL OR o.status = :status)
        """)
    Page<Order> searchOrdersAdmin(
            @Param("orderCode") String orderCode,
            @Param("status") OrderStatus status,
            Pageable pageable);
}
