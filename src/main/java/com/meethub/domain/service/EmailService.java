
package com.meethub.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;

import java.util.List;
import java.util.Map;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateEngine templateEngine;

    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("olayakym0@gmail.com");

            javaMailSender.send(message);

            log.info(" HTML email wysłany do: {}", to);

        } catch (Exception e) {
            log.error(" Błąd podczas wysyłki HTML email do {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }


    public void sendHtmlEmail(String to, String subject, String htmlContent,
                              String replyTo, List<String> cc, List<String> bcc) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("olayakym0@gmail.com");

            if (replyTo != null) {
                helper.setReplyTo(replyTo);
            }

            if (cc != null && !cc.isEmpty()) {
                helper.setCc(cc.toArray(new String[0]));
            }

            if (bcc != null && !bcc.isEmpty()) {
                helper.setBcc(bcc.toArray(new String[0]));
            }

            javaMailSender.send(message);

            log.info(" HTML email wysłany do: {} (CC: {}, BCC: {})",
                    to, cc != null ? cc.size() : 0, bcc != null ? bcc.size() : 0);

        } catch (Exception e) {
            log.error(" Błąd podczas wysyłki HTML email do {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendTemplateEmail(String to, String subject, String templateName,
                                  Map<String, Object> variables) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            Context context = new Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process("email/" + templateName, context);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            helper.setFrom("olayakym0@gmail.com");

            javaMailSender.send(message);

            log.info(" Template email wysłany do: {}", to);

        } catch (Exception e) {
            log.error(" Błąd podczas wysyłki template email do {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            helper.setFrom("olayakym0@gmail.com");

            javaMailSender.send(message);

            log.info(" Text email wysłany do: {}", to);

        } catch (Exception e) {
            log.error(" Błąd podczas wysyłki text email do {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}

