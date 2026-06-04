package com.mockinvest.domain.user;

import com.mockinvest.infrastructure.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            tokenRepository.invalidatePendingTokens(user);
            String token = UUID.randomUUID().toString();
            tokenRepository.save(PasswordResetToken.create(user, token));
            emailService.sendPasswordReset(user.getEmail(), token);
        });
        // 이메일 미존재 시에도 동일하게 처리 — 이메일 존재 여부 노출 방지
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 링크입니다."));
        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("이미 사용된 링크입니다.");
        }
        if (resetToken.isExpired()) {
            throw new IllegalArgumentException("만료된 링크입니다. 다시 요청해주세요.");
        }
        resetToken.markUsed();
        resetToken.getUser().updatePassword(passwordEncoder.encode(newPassword));
    }
}
