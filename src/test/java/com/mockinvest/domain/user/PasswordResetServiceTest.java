package com.mockinvest.domain.user;

import com.mockinvest.infrastructure.email.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock EmailService emailService;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks PasswordResetService passwordResetService;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = User.create("alice", "alice@example.com", "encoded", null);
        alice.verify();
    }

    @Test
    void requestReset_이메일_존재하면_토큰_생성_및_발송() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(tokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        passwordResetService.requestReset("alice@example.com");

        verify(tokenRepository).invalidatePendingTokens(alice);
        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordReset(eq("alice@example.com"), anyString());
    }

    @Test
    void requestReset_이메일_없으면_아무것도_안_함() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        passwordResetService.requestReset("unknown@example.com");

        verifyNoInteractions(emailService, tokenRepository);
    }

    @Test
    void resetPassword_성공() {
        PasswordResetToken token = PasswordResetToken.create(alice, "valid-token");
        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(passwordEncoder.encode("newpassword")).thenReturn("new-encoded");

        passwordResetService.resetPassword("valid-token", "newpassword");

        assertThat(token.isUsed()).isTrue();
        assertThat(alice.getPassword()).isEqualTo("new-encoded");
    }

    @Test
    void resetPassword_잘못된_토큰_예외() {
        when(tokenRepository.findByToken("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> passwordResetService.resetPassword("bad", "newpw"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은");
    }

    @Test
    void resetPassword_이미_사용된_토큰_예외() {
        PasswordResetToken token = PasswordResetToken.create(alice, "used-token");
        token.markUsed();
        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("used-token", "newpw"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 사용");
    }

    @Test
    void resetPassword_만료된_토큰_예외() {
        PasswordResetToken token = PasswordResetToken.create(alice, "expired-token");
        ReflectionTestUtils.setField(token, "expiresAt", LocalDateTime.now().minusHours(1));
        when(tokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> passwordResetService.resetPassword("expired-token", "newpw"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("만료");
    }
}
