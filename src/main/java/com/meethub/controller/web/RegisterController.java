
package com.meethub.controller.web;

import com.meethub.domain.model.request.UserRegistrationRequest;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Validated
@Slf4j
@Controller
@RequiredArgsConstructor
public class RegisterController {

    private final AuthService authService;

    @GetMapping("/register")
    @Operation(summary = "Formularz rejestracji użytkownika", description = "Wyświetla formularz rejestracji nowego użytkownika.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Formularz rejestracji wyświetlony pomyślnie")
    })
    public String showRegistrationForm(Model model) {
        log.info("Wyświetlanie formularza rejestracji");
        model.addAttribute("registrationRequest", new UserRegistrationRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    @Operation(summary = "Rejestracja nowego użytkownika", description = "Przetwarza dane z formularza rejestracji i tworzy nowego użytkownika.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "302", description = "Rejestracja udana, przekierowanie do logowania"),
            @ApiResponse(responseCode = "200", description = "Błąd walidacji formularza, wyświetlenie formularza z błędami")
    })
    public String registerUser(
            @Parameter(description = "Dane rejestracyjne użytkownika")
            @Valid @ModelAttribute("registrationRequest") UserRegistrationRequest request,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.info("Przetwarzanie rejestracji użytkownika: {}", request.getEmail());

        if (bindingResult.hasErrors()) {
            log.warn("Błędy walidacji formularza rejestracji: {}", bindingResult.getAllErrors());
            return "auth/register";
        }

        try {
            log.info("Próba rejestracji użytkownika: {}", request.getEmail());
            UserResponse user = authService.register(request);
            log.info("Użytkownik zarejestrowany pomyślnie: {} (ID: {})",
                    user.getEmail(), user.getId());

            redirectAttributes.addFlashAttribute("success",
                    "Rejestracja udana! Możesz się teraz zalogować.");
            log.info("Przekierowanie do /login po udanej rejestracji");

            return "redirect:/login";

        } catch (jakarta.validation.ConstraintViolationException e) {
            log.warn("Błąd walidacji podczas rejestracji: {}", e.getMessage());
            model.addAttribute("error", "Nieprawidłowe dane w formularzu rejestracji");
            return "auth/register";

        } catch (Exception e) {
            log.error("Rejestracja nieudana dla: {}", request.getEmail(), e);
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }
}

