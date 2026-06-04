package com.mockinvest.domain.user;

import com.mockinvest.infrastructure.email.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock EmailService emailService;

    @InjectMocks UserService userService;

    @Test
    void register_성공() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        User user = userService.register("alice", "alice@example.com", "pw");

        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getVerificationToken()).isNotNull();
        verify(emailService).sendVerification(eq("alice@example.com"), anyString());
    }

    @Test
    void register_중복_아이디_예외() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> userService.register("alice", "alice@example.com", "pw"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("아이디");
    }

    @Test
    void register_중복_이메일_예외() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register("alice", "alice@example.com", "pw"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이메일");
    }

    @Test
    void verifyEmail_성공() {
        User user = User.create("alice", "alice@example.com", "encoded", "token-123");
        when(userRepository.findByVerificationToken("token-123")).thenReturn(Optional.of(user));

        userService.verifyEmail("token-123");

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.getVerificationToken()).isNull();
    }

    @Test
    void verifyEmail_잘못된_토큰_예외() {
        when(userRepository.findByVerificationToken("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.verifyEmail("bad"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("유효하지 않은");
    }

    @Test
    void loadUserByUsername_미인증_계정은_disabled() {
        User user = User.create("alice", "alice@example.com", "encoded", "token-123");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = userService.loadUserByUsername("alice");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_인증된_계정은_enabled() {
        User user = User.create("alice", "alice@example.com", "encoded", "token-123");
        user.verify();
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = userService.loadUserByUsername("alice");

        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    void resendVerification_미인증_유저는_토큰_갱신() {
        User user = User.create("alice", "alice@example.com", "encoded", "old-token");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        userService.resendVerification("alice@example.com");

        assertThat(user.getVerificationToken()).isNotEqualTo("old-token");
        verify(emailService).sendVerification(eq("alice@example.com"), anyString());
    }

    @Test
    void resendVerification_이미_인증된_유저는_무시() {
        User user = User.create("alice", "alice@example.com", "encoded", "token");
        user.verify();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        userService.resendVerification("alice@example.com");

        verifyNoInteractions(emailService);
    }
}
