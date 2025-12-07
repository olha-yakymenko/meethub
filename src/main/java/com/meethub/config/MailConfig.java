// src/main/java/com/meethub/config/MailConfig.java
package com.meethub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // Konfiguracja Gmail (lub innego SMTP)
        mailSender.setHost("smtp.gmail.com");
        mailSender.setPort(587);

        // Jeśli nie chcesz podawać prawdziwych danych:
        mailSender.setUsername("olayakym0@gmail.com");
        mailSender.setPassword("zlym kdwq yqjg klqv");

        // Właściwości JavaMail
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true"); // włącz dla debugowania

        return mailSender;
    }
}