//package com.meethub;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.core.env.Environment;
//import org.springframework.mail.SimpleMailMessage;
//import org.springframework.mail.javamail.JavaMailSender;
//import org.springframework.stereotype.Component;
//
//import java.util.Properties;
//
//@Slf4j
//@Component
//public class EmailTest implements CommandLineRunner {
//
//    private final JavaMailSender mailSender;
//    private final Environment environment;
//
//    public EmailTest(JavaMailSender mailSender, Environment environment) {
//        this.mailSender = mailSender;
//        this.environment = environment;
//    }
//
//    @Override
//    public void run(String... args) {
//        log.info("\n" +
//                "╔══════════════════════════════════════════════╗\n" +
//                "║           TEST KONFIGURACJI EMAIL            ║\n" +
//                "╚══════════════════════════════════════════════╝");
//
//        logEmailConfiguration();
//
//        // Test wysłania
//        try {
//            sendTestEmail();
//            log.info("✅ Konfiguracja email działa poprawnie!");
//        } catch (Exception e) {
//            log.error("❌ Błąd konfiguracji email: {}", e.getMessage());
//            log.debug("Szczegóły błędu:", e);
//        }
//    }
//
//    private void logEmailConfiguration() {
//        try {
//            // 1. Pobierz właściwości z Environment (application.properties)
//            String host = environment.getProperty("spring.mail.host");
//            String port = environment.getProperty("spring.mail.port");
//            String username = environment.getProperty("spring.mail.username");
//            String password = environment.getProperty("spring.mail.password");
//
//            log.info("📋 KONFIGURACJA Z application.properties:");
//            log.info("   Host: {}", host);
//            log.info("   Port: {}", port);
//            log.info("   Username: {}", username);
//            log.info("   Password: {}", maskPassword(password));
//            log.info("   Password length: {}", password != null ? password.length() : "null");
////
////            // 2. Pobierz właściwości z JavaMailSender
////            if (mailSender != null) {
////                Properties props = mailSender.getJavaMailProperties();
////                log.info("\n📧 KONFIGURACJA JavaMailSender:");
////                log.info("   mail.host: {}", props.getProperty("mail.host"));
////                log.info("   mail.port: {}", props.getProperty("mail.port"));
////                log.info("   mail.username: {}", props.getProperty("mail.username"));
////                log.info("   mail.password: {}", maskPassword(props.getProperty("mail.password")));
////                log.info("   mail.smtp.auth: {}", props.getProperty("mail.smtp.auth"));
////                log.info("   mail.smtp.starttls.enable: {}", props.getProperty("mail.smtp.starttls.enable"));
////
////                // Sprawdź Session
////                var session = mailSender.getSession();
////                if (session != null) {
////                    Properties sessionProps = session.getProperties();
////                    log.info("\n🔐 KONFIGURACJA Session:");
////                    log.info("   mail.smtp.user: {}", sessionProps.getProperty("mail.smtp.user"));
////                    log.info("   mail.smtp.from: {}", sessionProps.getProperty("mail.smtp.from"));
////
////                    // Pobierz Authenticator jeśli istnieje
////                    var authenticator = session.getAuthenticator();
////                    if (authenticator != null) {
////                        log.info("   Authenticator: {}", authenticator.getClass().getName());
////                    }
////                }
////            }
//
//        } catch (Exception e) {
//            log.error("❌ Błąd podczas czytania konfiguracji: {}", e.getMessage());
//        }
//    }
//
//    private String maskPassword(String password) {
//        if (password == null || password.isEmpty()) {
//            return "null/empty";
//        }
//        // Pokazuj tylko pierwsze 3 i ostatnie 3 znaki
//        if (password.length() > 6) {
//            return password.substring(0, 3) + "***" + password.substring(password.length() - 3);
//        } else {
//            return "***"; // Dla krótkich haseł
//        }
//    }
//
//    private void sendTestEmail() {
//        SimpleMailMessage message = new SimpleMailMessage();
//        message.setTo("ola0@gmail.com");
//        message.setSubject("Test konfiguracji MeetHub - " + java.time.LocalDateTime.now());
//        message.setText("Jeśli to widzisz, konfiguracja email działa!");
//
//        // Opcjonalnie ustaw FROM
//        message.setFrom("noreply@meethub.com");
//
//        mailSender.send(message);
//        log.info("📤 Email wysłany do: ola0@gmail.com");
//    }
//}