package com.mockinvest.domain.portfolio;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DividendPayoutRepository extends JpaRepository<DividendPayout, Long> {

    Optional<DividendPayout> findByUserIdAndStockIdAndPayoutDate(Long userId, Long stockId, LocalDate date);
}
