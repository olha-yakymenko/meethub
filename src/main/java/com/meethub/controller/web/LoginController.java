package com.meethub.controller.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j // ✅ DODAJ to
@Controller
public class LoginController {

    @GetMapping("/login")
    public String loginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "expired", required = false) String expired,
            Model model) {

        log.info("🌐 GET /login called with params: error={}, logout={}, expired={}",
                error, logout, expired);

        if (error != null) {
            log.warn("❌ Login error detected - showing error message");
            model.addAttribute("error", "Nieprawidłowy email lub hasło");
        }

        if (logout != null) {
            log.info("👋 User logged out - showing logout message");
            model.addAttribute("message", "Zostałeś wylogowany pomyślnie");
        }

        if (expired != null) {
            log.warn("⏰ Session expired - showing expired message");
            model.addAttribute("error", "Sesja wygasła, zaloguj się ponownie");
        }

        log.info("📄 Returning login.html template");
        return "auth/login";
    }
}