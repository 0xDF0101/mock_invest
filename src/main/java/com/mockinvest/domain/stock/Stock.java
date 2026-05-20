package com.mockinvest.domain.stock;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stocks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String ticker;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Market market;

    public enum Market {
        KOSPI, KOSDAQ
    }

    public static Stock of(String ticker, String name, Market market) {
        Stock stock = new Stock();
        stock.ticker = ticker;
        stock.name = name;
        stock.market = market;
        return stock;
    }

    void updateName(String koreanName) {
        this.name = koreanName;
    }
}
