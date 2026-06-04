package com.mockinvest.infrastructure.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender,
                        @Value("${spring.mail.host:}") String mailHost) {
        // mailHost가 비어있으면 SMTP 미설정 → dev 모드로 동작 (링크를 로그로 출력)
        this.mailSender = !mailHost.isBlank() ? mailSender : null;
    }

    public void sendVerification(String to, String token) {
        String link = baseUrl + "/auth/verify?token=" + token;
        if (mailSender == null) {
            log.info("[DEV] 이메일 인증 링크 ({}): {}", to, link);
            return;
        }
        send(to, "[MockInvest] 이메일 인증",
                "아래 링크를 클릭하여 이메일 인증을 완료해주세요.\n\n" + link +
                "\n\n링크는 발급 후 유효합니다. 본인이 요청하지 않았다면 무시하셔도 됩니다.");
    }

    public void sendPasswordReset(String to, String token) {
        String link = baseUrl + "/auth/reset-password?token=" + token;
        if (mailSender == null) {
            log.info("[DEV] 비밀번호 재설정 링크 ({}): {}", to, link);
            return;
        }
        send(to, "[MockInvest] 비밀번호 재설정",
                "아래 링크를 클릭하여 비밀번호를 재설정해주세요. (24시간 유효)\n\n" + link +
                "\n\n본인이 요청하지 않았다면 무시하셔도 됩니다.");
    }

    private void send(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("이메일 발송 실패: to={}, subject={}", to, subject, e);
        }
    }
}
