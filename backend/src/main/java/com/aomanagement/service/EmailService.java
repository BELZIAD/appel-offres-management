package com.aomanagement.service;

import lombok.RequiredArgsConstructor;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final EmailTemplateService emailTemplateService;

    @Value("${app.mail.from}")
    private String from;

    public void send(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(emailTemplateService.render(subject, body), true);
            mailSender.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Email impossible", ex);
        }
    }
}
