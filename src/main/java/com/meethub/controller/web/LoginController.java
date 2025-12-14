package com.meethub.controller.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Validated // DODANE - walidacja dla kontrolera webowego
@Slf4j
@Controller
@Tag(name = "Logowanie", description = "Strony web do zarządzania procesem logowania użytkowników")
public class LoginController {

    @GetMapping("/login")
    @Operation(
            summary = "Strona logowania",
            description = "Wyświetla stronę logowania i obsługuje komunikaty o błędzie, wylogowaniu lub wygaśnięciu sesji."
    )
    public String loginPage(
            @Parameter(description = "Błąd logowania - pojawia się, gdy podano nieprawidłowy email lub hasło")
            @RequestParam(value = "error", required = false)
            @Size(max = 50, message = "Parametr błędu nie może przekraczać 50 znaków")
            String error,

            @Parameter(description = "Komunikat o wylogowaniu - pojawia się po pomyślnym wylogowaniu")
            @RequestParam(value = "logout", required = false)
            @Size(max = 50, message = "Parametr wylogowania nie może przekraczać 50 znaków")
            String logout,

            @Parameter(description = "Komunikat o wygasłej sesji - pojawia się, gdy sesja użytkownika wygasła")
            @RequestParam(value = "expired", required = false)
            @Size(max = 50, message = "Parametr wygaśnięcia sesji nie może przekraczać 50 znaków")
            String expired,

            Model model) {

        try {
            log.info("Wywołanie GET /login z parametrami: error={}, logout={}, expired={}", error, logout, expired);

            if (error != null) {
                log.warn("Wykryto błąd logowania - wyświetlanie komunikatu błędu");
                model.addAttribute("error", "Nieprawidłowy email lub hasło");
            }

            if (logout != null) {
                log.info("Użytkownik wylogowany - wyświetlanie komunikatu wylogowania");
                model.addAttribute("message", "Zostałeś wylogowany pomyślnie");
            }

            if (expired != null) {
                log.warn("Sesja wygasła - wyświetlanie komunikatu wygaśnięcia");
                model.addAttribute("error", "Sesja wygasła, zaloguj się ponownie");
            }

            log.info("Zwracanie szablonu auth/login");
            return "auth/login";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji parametrów logowania: {}", e.getMessage());
            // W przypadku błędnej walidacji parametrów, po prostu wyświetlamy czystą stronę logowania
            return "auth/login";

        } catch (Exception e) {
            log.error("Nieoczekiwany błąd podczas ładowania strony logowania", e);
            model.addAttribute("error", "Wystąpił błąd podczas ładowania strony logowania");
            return "auth/login";
        }
    }

    @GetMapping("/access-denied")
    @Operation(
            summary = "Strona braku dostępu",
            description = "Wyświetla stronę informującą o braku uprawnień do dostępu do żądanego zasobu."
    )
    public String accessDeniedPage(
            Model model) {

        log.warn("Wyświetlanie strony braku dostępu");
        model.addAttribute("error", "Nie masz uprawnień do dostępu do tego zasobu");
        return "auth/access-denied";
    }

    @GetMapping("/login?expired=true")
    @Operation(
            summary = "Przekierowanie na stronę logowania po wygaśnięciu sesji",
            description = "Automatyczne przekierowanie na stronę logowania z komunikatem o wygaśnięciu sesji."
    )
    public String sessionExpiredRedirect() {
        log.info("Przekierowanie na stronę logowania po wygaśnięciu sesji");
        return "redirect:/login?expired=true";
    }

    @GetMapping("/login?logout=true")
    @Operation(
            summary = "Przekierowanie na stronę logowania po wylogowaniu",
            description = "Automatyczne przekierowanie na stronę logowania z komunikatem o pomyślnym wylogowaniu."
    )
    public String logoutRedirect() {
        log.info("Przekierowanie na stronę logowania po wylogowaniu");
        return "redirect:/login?logout=true";
    }
}








//package com.meethub.controller.web;
//
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//
//@Slf4j
//@Controller
//@Tag(name = "Logowanie", description = "Zarządzanie procesem logowania użytkowników")
//public class LoginController {
//
//    @GetMapping("/login")
//    @Operation(
//            summary = "Strona logowania",
//            description = "Wyświetla stronę logowania i obsługuje komunikaty o błędzie, wylogowaniu lub wygaśnięciu sesji."
//    )
//    public String loginPage(
//            @Parameter(description = "Błąd logowania - pojawia się, gdy podano nieprawidłowy email lub hasło")
//            @RequestParam(value = "error", required = false) String error,
//
//            @Parameter(description = "Komunikat o wylogowaniu - pojawia się po pomyślnym wylogowaniu")
//            @RequestParam(value = "logout", required = false) String logout,
//
//            @Parameter(description = "Komunikat o wygasłej sesji - pojawia się, gdy sesja użytkownika wygasła")
//            @RequestParam(value = "expired", required = false) String expired,
//
//            Model model) {
//
//        log.info("GET /login called with params: error={}, logout={}, expired={}", error, logout, expired);
//
//        if (error != null) {
//            log.warn("Login error detected - showing error message");
//            model.addAttribute("error", "Nieprawidłowy email lub hasło");
//        }
//
//        if (logout != null) {
//            log.info("User logged out - showing logout message");
//            model.addAttribute("message", "Zostałeś wylogowany pomyślnie");
//        }
//
//        if (expired != null) {
//            log.warn("Session expired - showing expired message");
//            model.addAttribute("error", "Sesja wygasła, zaloguj się ponownie");
//        }
//
//        log.info("Returning auth/login template");
//        return "auth/login";
//    }
//}
