package com.mockinvest.domain.user;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private BigDecimal cashBalance;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static User create(String username, String email, String encodedPassword) {
        User user = new User();
        user.username = username;
        user.email = email;
        user.password = encodedPassword;
        user.cashBalance = new BigDecimal("10000000");
        return user;
    }

    public void deductCash(BigDecimal amount) {
        if (this.cashBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("잔액이 부족합니다.");
        }
        this.cashBalance = this.cashBalance.subtract(amount);
    }

    public void addCash(BigDecimal amount) {
        this.cashBalance = this.cashBalance.add(amount);
    }
}
