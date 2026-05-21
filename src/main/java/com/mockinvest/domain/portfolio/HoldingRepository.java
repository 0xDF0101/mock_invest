package com.mockinvest.domain.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    Optional<Holding> findByUserIdAndStockId(Long userId, Long stockId);

    @Query("SELECT h FROM Holding h JOIN FETCH h.stock WHERE h.user.id = :userId AND h.quantity > 0")
    List<Holding> findActiveHoldingsByUserId(Long userId);

    @Query("SELECT h FROM Holding h JOIN FETCH h.stock JOIN FETCH h.user WHERE h.quantity > 0")
    List<Holding> findAllActiveHoldings();
}
