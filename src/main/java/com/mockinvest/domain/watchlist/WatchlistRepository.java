package com.mockinvest.domain.watchlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    boolean existsByUserIdAndStockId(Long userId, Long stockId);

    void deleteByUserIdAndStockId(Long userId, Long stockId);

    @Query("SELECT w FROM Watchlist w JOIN FETCH w.stock WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<Watchlist> findByUserIdWithStock(Long userId);
}
