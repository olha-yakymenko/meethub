package com.meethub.controller.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@Tag(name = "Logowanie", description = "Zarządzanie procesem logowania użytkowników")
public class LoginController {

    @GetMapping("/login")
    @Operation(
            summary = "Strona logowania",
            description = "Wyświetla stronę logowania i obsługuje komunikaty o błędzie, wylogowaniu lub wygaśnięciu sesji."
    )
    public String loginPage(
            @Parameter(description = "Błąd logowania - pojawia się, gdy podano nieprawidłowy email lub hasło")
            @RequestParam(value = "error", required = false) String error,

            @Parameter(description = "Komunikat o wylogowaniu - pojawia się po pomyślnym wylogowaniu")
            @RequestParam(value = "logout", required = false) String logout,

            @Parameter(description = "Komunikat o wygasłej sesji - pojawia się, gdy sesja użytkownika wygasła")
            @RequestParam(value = "expired", required = false) String expired,

            Model model) {

        log.info("GET /login called with params: error={}, logout={}, expired={}", error, logout, expired);

        if (error != null) {
            log.warn("Login error detected - showing error message");
            model.addAttribute("error", "Nieprawidłowy email lub hasło");
        }

        if (logout != null) {
            log.info("User logged out - showing logout message");
            model.addAttribute("message", "Zostałeś wylogowany pomyślnie");
        }

        if (expired != null) {
            log.warn("Session expired - showing expired message");
            model.addAttribute("error", "Sesja wygasła, zaloguj się ponownie");
        }

        log.info("Returning auth/login template");
        return "auth/login";
    }
}
