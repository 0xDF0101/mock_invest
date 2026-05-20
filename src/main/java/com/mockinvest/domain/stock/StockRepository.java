package com.mockinvest.domain.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {
    Optional<Stock> findByTicker(String ticker);

    // 한글/영문 이름 OR 티커 동시 검색
    List<Stock> findByNameContainingIgnoreCaseOrTickerContainingIgnoreCase(String name, String ticker);

    @Query(value = "SELECT * FROM stocks ORDER BY RAND() LIMIT :limit", nativeQuery = true)
    List<Stock> findRandom(@Param("limit") int limit);
}
