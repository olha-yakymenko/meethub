package com.meethub.exception;

import com.meethub.domain.repository.jdbc.CustomMeetingRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
//@RestControllerAdvice(annotations = RestController.class)
@RestControllerAdvice
public class GlobalExceptionHandler {

    // DTO dla odpowiedzi błędów
    @Data
    @Builder
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, String> details;
    }

    // ---------------------- Obsługa wyjątków bazy danych ----------------------

    @ExceptionHandler(CustomMeetingRepository.RepositoryException.class)
    public ResponseEntity<ErrorResponse> handleRepositoryException(
            CustomMeetingRepository.RepositoryException ex, WebRequest request) {

        log.error("Repository error - Operation: {}, SQL: {}", ex.getOperation(), ex.getSql(), ex);

        Map<String, String> details = new HashMap<>();
        details.put("operation", ex.getOperation());
        details.put("sql", ex.getSql());

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Błąd dostępu do danych. Proszę spróbować później.",
                request,
                details
        );
    }
//
//    @ExceptionHandler(DataAccessException.class)
//    public ResponseEntity<ErrorResponse> handleDataAccessException(
//            DataAccessException ex, WebRequest request) {
//
//        log.error("Database access error: {}", ex.getMessage(), ex);
//
//        return buildErrorResponse(
//                HttpStatus.INTERNAL_SERVER_ERROR,
//                "Błąd dostępu do bazy danych. Proszę spróbować później.",
//                request,
//                null
//        );
//    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<ErrorResponse> handleEmptyResultDataAccessException(
            EmptyResultDataAccessException ex, WebRequest request) {

        log.info("No results found: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Nie znaleziono żądanych danych.",
                request,
                null
        );
    }

    // ---------------------- Obsługa wyjątków serwisowych ----------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            ValidationException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex, WebRequest request) {
        log.warn("Business error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // ---------------------- Obsługa walidacji ----------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ?
                                fieldError.getDefaultMessage() : "Invalid value"
                ));

        log.warn("Method argument validation failed: {}", errors);

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Walidacja danych nie powiodła się",
                request,
                errors
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        Map<String, String> errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage()
                ));

        log.warn("Constraint violation: {}", errors);

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Walidacja danych nie powiodła się",
                request,
                errors
        );
    }

    // ---------------------- Obsługa argumentów i stanu ----------------------

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, WebRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // ---------------------- Obsługa nieoczekiwanych błędów ----------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Wystąpił nieoczekiwany błąd serwera",
                request,
                null
        );
    }

    // ---------------------- Wspólna metoda budowania odpowiedzi ----------------------

    private ResponseEntity<ErrorResponse> buildErrorResponse(
            HttpStatus status,
            String message,
            WebRequest request,
            Map<String, String> details) {

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request != null ? request.getDescription(false).replace("uri=", "") : "")
                .details(details)
                .build();

        return new ResponseEntity<>(error, status);
    }


    @ExceptionHandler(DataAccessException.class)
    public Object handleDataAccessException(
            DataAccessException ex,
            WebRequest request,
            HttpServletRequest httpRequest) {

        String path = httpRequest.getRequestURI();

        // Jeśli to web endpoint (/meetings/...), przekieruj do strony błędu
        if (path != null && !path.startsWith("/api/")) {
            log.error("Web database error for path {}: {}", path, ex.getMessage(), ex);

            ModelAndView mav = new ModelAndView("error");
            mav.addObject("error", "Błąd dostępu do bazy danych");
            mav.addObject("message", "Proszę spróbować później");
            mav.addObject("status", 500);
            mav.addObject("path", path);
            mav.addObject("timestamp", LocalDateTime.now());

            return mav;
        }

        // Dla API endpointów zwróć JSON
        log.error("API database error: {}", ex.getMessage(), ex);

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Błąd dostępu do bazy danych. Proszę spróbować później.",
                request,
                null
        );

    }


    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, WebRequest request) {

        log.warn("Type mismatch for parameter '{}': expected {}, got {}",
                ex.getName(), ex.getRequiredType(), ex.getValue());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Nieprawidłowy typ parametru: " + ex.getName(),
                request,
                null
        );
    }
}