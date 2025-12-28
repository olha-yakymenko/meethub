//package com.meethub.exception;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.validation.ConstraintViolationException;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.validation.FieldError;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.context.request.WebRequest;
//import org.springframework.web.servlet.resource.NoResourceFoundException;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@ControllerAdvice(annotations = Controller.class)
//@Slf4j
//public class WebExceptionHandler {
//
//    @ExceptionHandler(NoResourceFoundException.class)
//    public String handle404(
//            NoResourceFoundException ex,
//            HttpServletRequest request,
//            Model model) {
//
//        fillModel(model, request, 404, "Strona nie została znaleziona");
//        return "error";
//    }
//
//    @ExceptionHandler(Exception.class)
//    public String handle500(
//            Exception ex,
//            HttpServletRequest request,
//            Model model) {
//
//        log.error("WEB error", ex);
//        fillModel(model, request, 500, "Wystąpił nieoczekiwany błąd");
//        return "error";
//    }
//
//    private void fillModel(
//            Model model,
//            HttpServletRequest request,
//            int status,
//            String message) {
//
//        model.addAttribute("timestamp", LocalDateTime.now());
//        model.addAttribute("status", status);
//        model.addAttribute("error", HttpStatus.valueOf(status).getReasonPhrase());
//        model.addAttribute("message", message);
//        model.addAttribute("path", request.getRequestURI());
//    }
//
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<GlobalExceptionHandler.ErrorResponse> handleMethodArgumentNotValid(
//            MethodArgumentNotValidException ex, WebRequest request) {
//
//        Map<String, String> errors = ex.getBindingResult().getFieldErrors()
//                .stream()
//                .collect(Collectors.toMap(
//                        FieldError::getField,
//                        fieldError -> fieldError.getDefaultMessage() != null ?
//                                fieldError.getDefaultMessage() : "Invalid value"
//                ));
//
//        log.warn("Method argument validation failed: {}", errors);
//
//        return buildErrorResponse(
//                HttpStatus.BAD_REQUEST,
//                "Walidacja danych nie powiodła się",
//                request,
//                errors
//        );
//    }
//
//    @ExceptionHandler(ConstraintViolationException.class)
//    public ResponseEntity<WebExceptionHandler.ErrorResponse> handleConstraintViolation(
//            ConstraintViolationException ex, WebRequest request) {
//
//        Map<String, String> errors = ex.getConstraintViolations()
//                .stream()
//                .collect(Collectors.toMap(
//                        v -> v.getPropertyPath().toString(),
//                        v -> v.getMessage()
//                ));
//
//        log.warn("Constraint violation: {}", errors);
//
//        return buildErrorResponse(
//                HttpStatus.BAD_REQUEST,
//                "Walidacja danych nie powiodła się",
//                request,
//                errors
//        );
//    }
//
//    private ResponseEntity<WebExceptionHandler.ErrorResponse> buildErrorResponse(
//            HttpStatus status,
//            String message,
//            WebRequest request,
//            Map<String, String> details) {
//
//        WebExceptionHandler.ErrorResponse error = GlobalExceptionHandler.ErrorResponse.builder()
//                .timestamp(LocalDateTime.now())
//                .status(status.value())
//                .error(status.getReasonPhrase())
//                .message(message)
//                .path(request != null ? request.getDescription(false).replace("uri=", "") : "")
//                .details(details)
//                .build();
//
//        return new ResponseEntity<>(error, status);
//    }
//}






//
//package com.meethub.exception;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.validation.ConstraintViolationException;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.validation.FieldError;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ControllerAdvice;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.context.request.WebRequest;
//import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//import org.springframework.web.servlet.resource.NoResourceFoundException;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@ControllerAdvice(annotations = Controller.class)
//@Slf4j
//public class WebExceptionHandler {
//
//    @ExceptionHandler(NoResourceFoundException.class)
//    public String handle404(
//            NoResourceFoundException ex,
//            HttpServletRequest request,
//            Model model) {
//
//        fillModel(model, request, 404, "Strona nie została znaleziona");
//        return "error";
//    }
//
//    @ExceptionHandler(Exception.class)
//    public String handle500(
//            Exception ex,
//            HttpServletRequest request,
//            Model model) {
//
//        log.error("WEB error", ex);
//        fillModel(model, request, 500, "Wystąpił nieoczekiwany błąd");
//        return "error";
//    }
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public String handleMethodArgumentNotValid(
//            MethodArgumentNotValidException ex,
//            HttpServletRequest request,
//            RedirectAttributes redirectAttributes) {
//
//        Map<String, String> errors = ex.getBindingResult().getFieldErrors()
//                .stream()
//                .collect(Collectors.toMap(
//                        FieldError::getField,
//                        fieldError -> fieldError.getDefaultMessage() != null ?
//                                fieldError.getDefaultMessage() : "Nieprawidłowa wartość"
//                ));
//
//        log.warn("Method argument validation failed: {}", errors);
//
//        // Pobierz pierwszą wiadomość błędu
//        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
//                .findFirst()
//                .map(FieldError::getDefaultMessage)
//                .orElse("Nieprawidłowe dane w formularzu");
//
//        // Dodaj jako flash attribute
//        redirectAttributes.addFlashAttribute("error", errorMessage);
//
//        // Przekieruj z powrotem do formularza
//        String requestUri = request.getRequestURI();
//        return "redirect:" + requestUri;
//    }
//
//    @ExceptionHandler(ConstraintViolationException.class)
//    public String handleConstraintViolation(
//            ConstraintViolationException ex,
//            HttpServletRequest request,
//            RedirectAttributes redirectAttributes) {
//
//        // Pobierz pierwszą wiadomość błędu
//        String errorMessage = ex.getConstraintViolations().stream()
//                .findFirst()
//                .map(violation -> violation.getMessage())
//                .orElse("Nieprawidłowe dane wejściowe");
//
//        log.warn("Constraint violation: {}", errorMessage);
//
//        // Dodaj jako flash attribute
//        redirectAttributes.addFlashAttribute("error", errorMessage);
//
//        // Przeanalizuj URL i przekieruj odpowiednio
//        String requestUri = request.getRequestURI();
//        String[] parts = requestUri.split("/");
//
//        try {
//            if (requestUri.contains("/meetings/") && requestUri.contains("/tasks/")) {
//                // Znajdź meetingId
//                String meetingId = null;
//                for (int i = 0; i < parts.length; i++) {
//                    if ("meetings".equals(parts[i]) && i + 1 < parts.length) {
//                        meetingId = parts[i + 1];
//                        break;
//                    }
//                }
//
//                if (meetingId != null) {
//                    if (requestUri.contains("/create")) {
//                        return "redirect:/meetings/" + meetingId + "/tasks/create";
//                    } else if (requestUri.contains("/edit")) {
//                        // Znajdź taskId
//                        String taskId = null;
//                        for (int i = 0; i < parts.length; i++) {
//                            if ("tasks".equals(parts[i]) && i + 1 < parts.length) {
//                                String potentialTaskId = parts[i + 1];
//                                // Sprawdź czy to numer (taskId)
//                                if (potentialTaskId.matches("\\d+")) {
//                                    taskId = potentialTaskId;
//                                    break;
//                                }
//                            }
//                        }
//                        if (taskId != null) {
//                            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId + "/edit";
//                        }
//                    }
//                    return "redirect:/meetings/" + meetingId + "/tasks";
//                }
//            } else if (requestUri.contains("/meetings/")) {
//                // Dla błędów w samym meetingId
//                return "redirect:/meetings";
//            }
//        } catch (Exception e) {
//            log.error("Błąd podczas parsowania URL", e);
//        }
//
//        // Domyślne przekierowanie
//        return "redirect:/meetings";
//    }
//
//    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
//    public String handleTypeMismatch(
//            MethodArgumentTypeMismatchException ex,
//            HttpServletRequest request,
//            RedirectAttributes redirectAttributes) {
//
//        log.warn("Type mismatch error: {}", ex.getMessage());
//        redirectAttributes.addFlashAttribute("error", "Nieprawidłowy format identyfikatora");
//
//        return "redirect:/meetings";
//    }
//
//    private void fillModel(
//            Model model,
//            HttpServletRequest request,
//            int status,
//            String message) {
//
//        model.addAttribute("timestamp", LocalDateTime.now());
//        model.addAttribute("status", status);
//        model.addAttribute("error", HttpStatus.valueOf(status).getReasonPhrase());
//        model.addAttribute("message", message);
//        model.addAttribute("path", request.getRequestURI());
//    }
//}









package com.meethub.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WebExceptionHandler {

    // ========== BŁĘDY HTTP ==========

    @ExceptionHandler(NoResourceFoundException.class)
    public String handle404(
            NoResourceFoundException ex,
            HttpServletRequest request,
            Model model) {

        log.warn("404 Not Found: {}", request.getRequestURI());
        fillModel(model, request, 404, "Strona nie została znaleziona");
        return "error";
    }

    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request,
            Model model) {

        log.warn("Access denied for {}: {}", request.getRequestURI(), ex.getMessage());
        fillModel(model, request, 403, "Brak uprawnień do tej strony");
        return "error";
    }

    // ========== WALIDACJA ==========

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Nieprawidłowe dane w formularzu");

        log.warn("Form validation failed: {}", errorMessage);

        // Zapisz błąd jako flash attribute
        redirectAttributes.addFlashAttribute("error", errorMessage);

        // Zapisz wszystkie błędy dla precyzyjnego wyświetlenia
        if (ex.getBindingResult().hasErrors()) {
            Map<String, String> allErrors = ex.getBindingResult().getFieldErrors().stream()
                    .collect(Collectors.toMap(
                            FieldError::getField,
                            fieldError -> fieldError.getDefaultMessage() != null ?
                                    fieldError.getDefaultMessage() : "Nieprawidłowa wartość"
                    ));
            redirectAttributes.addFlashAttribute("validationErrors", allErrors);
        }

        // Powrót do poprzedniej strony
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/meetings");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        String errorMessage = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("Nieprawidłowe dane wejściowe");

        log.warn("Constraint violation: {}", errorMessage);
        redirectAttributes.addFlashAttribute("error", errorMessage);

        return getRedirectUrl(request);
    }

    @ExceptionHandler(BindException.class)
    public String handleBindException(
            BindException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        String errorMessage = ex.getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Nieprawidłowe dane w formularzu");

        log.warn("Bind exception: {}", errorMessage);
        redirectAttributes.addFlashAttribute("error", errorMessage);

        if (ex.hasErrors()) {
            Map<String, String> errors = ex.getFieldErrors().stream()
                    .collect(Collectors.toMap(
                            FieldError::getField,
                            fieldError -> fieldError.getDefaultMessage() != null ?
                                    fieldError.getDefaultMessage() : "Nieprawidłowa wartość"
                    ));
            redirectAttributes.addFlashAttribute("validationErrors", errors);
        }

        return getRedirectUrl(request);
    }

    // ========== BŁĘDY TYPÓW ==========

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        log.warn("Type mismatch for parameter '{}': {}", ex.getName(), ex.getMessage());
        redirectAttributes.addFlashAttribute("error",
                String.format("Nieprawidłowa wartość parametru '%s'", ex.getName()));

        return getRedirectUrl(request);
    }

    // ========== BŁĘDY BIZNESOWE ==========

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        log.warn("Resource not found: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());

        return getRedirectUrl(request);
    }

    @ExceptionHandler(ValidationException.class)
    public String handleValidationException(
            ValidationException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        log.warn("Validation error: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());

        return getRedirectUrl(request);
    }

    @ExceptionHandler(BusinessException.class)
    public String handleBusinessException(
            BusinessException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        log.warn("Business error: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());

        return getRedirectUrl(request);
    }

    // ========== BŁĘDY BAZY DANYCH ==========

    @ExceptionHandler(DataAccessException.class)
    public String handleDataAccessException(
            DataAccessException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        log.error("Database error for {}: {}", request.getRequestURI(), ex.getMessage());
        redirectAttributes.addFlashAttribute("error",
                "Błąd dostępu do danych. Proszę spróbować ponownie.");

        return getRedirectUrl(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        log.warn("Illegal argument: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());

        return getRedirectUrl(request);
    }

    @ExceptionHandler(IllegalStateException.class)
    public String handleIllegalState(
            IllegalStateException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        log.warn("Illegal state: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", ex.getMessage());

        return getRedirectUrl(request);
    }

    // ========== OGÓLNE BŁĘDY ==========

//    @ExceptionHandler(RuntimeException.class)
//    public String handleRuntimeException(
//            RuntimeException ex,
//            HttpServletRequest request,
//            RedirectAttributes redirectAttributes) {
//
//        log.error("Runtime error for {}: {}", request.getRequestURI(), ex.getMessage(), ex);
//        redirectAttributes.addFlashAttribute("error",
//                "Wystąpił błąd aplikacji. Proszę spróbować ponownie.");
//
//        return getRedirectUrl(request);
//    }


    @ExceptionHandler(RuntimeException.class)
    public String handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        log.error("Web error", ex);

        String uri = request.getRequestURI();

        if (uri.matches(".*/resources/\\d+/details$")) {
            Long meetingId = extractMeetingId(uri);
            redirectAttributes.addAttribute(
                    "error", "Nie można wyświetlić szczegółów zasobu"
            );
            return "redirect:/meetings/" + meetingId + "/resources";
        }

        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/meetings";
    }

    private Long extractMeetingId(String uri) {
        // /meetings/{id}/resources/{rid}/details
        return Long.valueOf(uri.split("/")[2]);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unhandled error for {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ModelAndView mav = new ModelAndView("error");
        mav.addObject("timestamp", LocalDateTime.now());
        mav.addObject("status", 500);
        mav.addObject("error", "Wewnętrzny błąd serwera");
        mav.addObject("message", "Wystąpił nieoczekiwany błąd. Proszę spróbować później.");
        mav.addObject("path", request.getRequestURI());

        return mav;
    }

    // ========== POMOCNICZE METODY ==========

    String getRedirectUrl(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        String requestUri = request.getRequestURI();

        // Jeśli mamy referer, wróć tam
        if (referer != null && !referer.isEmpty()) {
            return "redirect:" + referer;
        }

        // W przeciwnym razie spróbuj rozpoznać kontekst
        if (requestUri.contains("/meetings/")) {
            // Wyciągnij meetingId
            String[] parts = requestUri.split("/");
            for (int i = 0; i < parts.length; i++) {
                if ("meetings".equals(parts[i]) && i + 1 < parts.length &&
                        parts[i + 1].matches("\\d+")) {
                    Long meetingId = Long.parseLong(parts[i + 1]);
                    return "redirect:/meetings/" + meetingId;
                }
            }
            return "redirect:/meetings";
        }

        // Domyślnie do dashboard
        return "redirect:/meetings";
    }

    private void fillModel(
            Model model,
            HttpServletRequest request,
            int status,
            String message) {

        model.addAttribute("timestamp", LocalDateTime.now());
        model.addAttribute("status", status);
        model.addAttribute("error", HttpStatus.valueOf(status).getReasonPhrase());
        model.addAttribute("message", message);
        model.addAttribute("path", request.getRequestURI());
    }
}