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

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column
    private String verificationToken;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static User create(String username, String email, String encodedPassword, String verificationToken) {
        User user = new User();
        user.username = username;
        user.email = email;
        user.password = encodedPassword;
        user.cashBalance = new BigDecimal("10000000");
        user.emailVerified = false;
        user.verificationToken = verificationToken;
        return user;
    }

    public void verify() {
        this.emailVerified = true;
        this.verificationToken = null;
    }

    public void renewVerificationToken(String token) {
        this.verificationToken = token;
    }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
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
