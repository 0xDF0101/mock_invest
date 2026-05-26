package com.mockinvest.domain.trade;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LimitOrderRepository extends JpaRepository<LimitOrder, Long> {

    @Query("SELECT o FROM LimitOrder o JOIN FETCH o.stock JOIN FETCH o.user WHERE o.status = 'PENDING'")
    List<LimitOrder> findAllPending();

    @Query("SELECT o FROM LimitOrder o JOIN FETCH o.stock WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    List<LimitOrder> findByUserIdOrderByCreatedAtDesc(Long userId);
}
