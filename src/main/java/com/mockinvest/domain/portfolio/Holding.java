package com.mockinvest.domain.portfolio;

import com.mockinvest.domain.stock.Stock;
import com.mockinvest.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "holdings")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private BigDecimal avgBuyPrice;

    public static Holding create(User user, Stock stock) {
        Holding h = new Holding();
        h.user = user;
        h.stock = stock;
        h.quantity = 0;
        h.avgBuyPrice = BigDecimal.ZERO;
        return h;
    }

    public void addShares(int qty, BigDecimal price) {
        BigDecimal totalCost = avgBuyPrice.multiply(BigDecimal.valueOf(quantity))
                .add(price.multiply(BigDecimal.valueOf(qty)));
        quantity += qty;
        avgBuyPrice = totalCost.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
    }

    public void removeShares(int qty) {
        if (quantity < qty) {
            throw new IllegalStateException("보유 수량이 부족합니다.");
        }
        quantity -= qty;
    }
}
