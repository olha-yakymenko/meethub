package com.meethub.controller.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Validated
@Slf4j
@Controller
@Tag(name = "Logowanie", description = "Strony web do zarządzania procesem logowania użytkowników")
public class LoginController {

    @GetMapping("/login")
    @Operation(
            summary = "Strona logowania",
            description = "Wyświetla stronę logowania i obsługuje komunikaty o błędzie, wylogowaniu lub wygaśnięciu sesji."
    )
    public String loginPage(@Validated @ModelAttribute LoginRequest request, Model model) {

        log.info("Wywołanie GET /login z parametrami: error={}, logout={}, expired={}",
                request.getError(), request.getLogout(), request.getExpired());

        if (request.getError() != null) {
            log.warn("Wykryto błąd logowania - wyświetlanie komunikatu błędu");
            model.addAttribute("error", "Nieprawidłowy email lub hasło");
        }

        if (request.getLogout() != null) {
            log.info("Użytkownik wylogowany - wyświetlanie komunikatu wylogowania");
            model.addAttribute("message", "Zostałeś wylogowany pomyślnie");
        }

        if (request.getExpired() != null) {
            log.warn("Sesja wygasła - wyświetlanie komunikatu wygaśnięcia");
            model.addAttribute("error", "Sesja wygasła, zaloguj się ponownie");
        }

        log.info("Zwracanie szablonu auth/login");
        return "auth/login";
    }

    @GetMapping("/access-denied")
    @Operation(
            summary = "Strona braku dostępu",
            description = "Wyświetla stronę informującą o braku uprawnień do dostępu do żądanego zasobu."
    )
    public String accessDeniedPage(Model model) {
        log.warn("Wyświetlanie strony braku dostępu");
        model.addAttribute("error", "Nie masz uprawnień do dostępu do tego zasobu");
        return "auth/access-denied";
    }

    // ===== Request DTO =====
    @Getter
    @Setter
    public static class LoginRequest {

        @Parameter(description = "Błąd logowania - pojawia się, gdy podano nieprawidłowy email lub hasło")
        @Size(max = 50, message = "Parametr błędu nie może przekraczać 50 znaków")
        private String error;

        @Parameter(description = "Komunikat o wylogowaniu - pojawia się po pomyślnym wylogowaniu")
        @Size(max = 50, message = "Parametr wylogowania nie może przekraczać 50 znaków")
        private String logout;

        @Parameter(description = "Komunikat o wygasłej sesji - pojawia się, gdy sesja użytkownika wygasła")
        @Size(max = 50, message = "Parametr wygaśnięcia sesji nie może przekraczać 50 znaków")
        private String expired;
    }
}
