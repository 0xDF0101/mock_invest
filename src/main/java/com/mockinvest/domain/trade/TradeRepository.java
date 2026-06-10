package com.mockinvest.domain.trade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    @Query("SELECT t FROM Trade t JOIN FETCH t.stock WHERE t.user.id = :userId ORDER BY t.createdAt DESC")
    Page<Trade> findByUserIdWithStock(Long userId, Pageable pageable);

    @Query("SELECT t FROM Trade t JOIN FETCH t.stock WHERE t.user.id = :userId ORDER BY t.createdAt ASC")
    List<Trade> findAllByUserIdWithStock(Long userId);
}
