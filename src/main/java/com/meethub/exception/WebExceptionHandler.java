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







package com.meethub.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice(annotations = Controller.class)
@Slf4j
public class WebExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public String handle404(
            NoResourceFoundException ex,
            HttpServletRequest request,
            Model model) {

        fillModel(model, request, 404, "Strona nie została znaleziona");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handle500(
            Exception ex,
            HttpServletRequest request,
            Model model) {

        log.error("WEB error", ex);
        fillModel(model, request, 500, "Wystąpił nieoczekiwany błąd");
        return "error";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        Map<String, String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ?
                                fieldError.getDefaultMessage() : "Nieprawidłowa wartość"
                ));

        log.warn("Method argument validation failed: {}", errors);

        // Pobierz pierwszą wiadomość błędu
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Nieprawidłowe dane w formularzu");

        // Dodaj jako flash attribute
        redirectAttributes.addFlashAttribute("error", errorMessage);

        // Przekieruj z powrotem do formularza
        String requestUri = request.getRequestURI();
        return "redirect:" + requestUri;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public String handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        // Pobierz pierwszą wiadomość błędu
        String errorMessage = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse("Nieprawidłowe dane wejściowe");

        log.warn("Constraint violation: {}", errorMessage);

        // Dodaj jako flash attribute
        redirectAttributes.addFlashAttribute("error", errorMessage);

        // Przeanalizuj URL i przekieruj odpowiednio
        String requestUri = request.getRequestURI();
        String[] parts = requestUri.split("/");

        try {
            if (requestUri.contains("/meetings/") && requestUri.contains("/tasks/")) {
                // Znajdź meetingId
                String meetingId = null;
                for (int i = 0; i < parts.length; i++) {
                    if ("meetings".equals(parts[i]) && i + 1 < parts.length) {
                        meetingId = parts[i + 1];
                        break;
                    }
                }

                if (meetingId != null) {
                    if (requestUri.contains("/create")) {
                        return "redirect:/meetings/" + meetingId + "/tasks/create";
                    } else if (requestUri.contains("/edit")) {
                        // Znajdź taskId
                        String taskId = null;
                        for (int i = 0; i < parts.length; i++) {
                            if ("tasks".equals(parts[i]) && i + 1 < parts.length) {
                                String potentialTaskId = parts[i + 1];
                                // Sprawdź czy to numer (taskId)
                                if (potentialTaskId.matches("\\d+")) {
                                    taskId = potentialTaskId;
                                    break;
                                }
                            }
                        }
                        if (taskId != null) {
                            return "redirect:/meetings/" + meetingId + "/tasks/" + taskId + "/edit";
                        }
                    }
                    return "redirect:/meetings/" + meetingId + "/tasks";
                }
            } else if (requestUri.contains("/meetings/")) {
                // Dla błędów w samym meetingId
                return "redirect:/meetings";
            }
        } catch (Exception e) {
            log.error("Błąd podczas parsowania URL", e);
        }

        // Domyślne przekierowanie
        return "redirect:/meetings";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String handleTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        log.warn("Type mismatch error: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("error", "Nieprawidłowy format identyfikatora");

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