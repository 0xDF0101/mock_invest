package com.mockinvest.infrastructure.email;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;

    private EmailService buildService(JavaMailSender sender, String host) {
        EmailService service = new EmailService(sender, host);
        ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(service, "fromAddress", "noreply@example.com");
        return service;
    }

    @Test
    void sendVerification_dev모드_예외없이_링크_로깅() {
        EmailService service = buildService(null, ""); // MAIL_HOST 미설정

        assertThatNoException().isThrownBy(() ->
                service.sendVerification("user@example.com", "token-123"));
    }

    @Test
    void sendVerification_prod모드_메일_발송() {
        EmailService service = buildService(mailSender, "smtp.gmail.com");

        service.sendVerification("user@example.com", "token-123");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendPasswordReset_dev모드_예외없이_링크_로깅() {
        EmailService service = buildService(null, "");

        assertThatNoException().isThrownBy(() ->
                service.sendPasswordReset("user@example.com", "reset-token"));
    }

    @Test
    void sendPasswordReset_prod모드_메일_발송() {
        EmailService service = buildService(mailSender, "smtp.gmail.com");

        service.sendPasswordReset("user@example.com", "reset-token");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
