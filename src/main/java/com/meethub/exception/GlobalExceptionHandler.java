package com.meethub.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ✅ DTO dla odpowiedzi błędów
    @Data
    @Builder
    public static class ErrorResponse {
        private LocalDateTime timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private Map<String, String> errors;
    }

    // ---------------------- Obsługa wyjątków serwisowych ----------------------
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex, WebRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, WebRequest request) {
        log.warn("Business error: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // ---------------------- Obsługa walidacji ----------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage
                ));
        log.warn("Method argument validation failed: {}", errors);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Walidacja danych nie powiodła się", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        Map<String, String> errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage()
                ));
        log.warn("Constraint violation: {}", errors);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Walidacja danych nie powiodła się", request, errors);
    }

    // ---------------------- Obsługa argumentów i stanu ----------------------
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, WebRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request, null);
    }

    // ---------------------- Obsługa nieoczekiwanych błędów ----------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                "Wystąpił nieoczekiwany błąd serwera", request, null);
    }

    // ---------------------- Wspólna metoda budowania odpowiedzi ----------------------
    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status, String message, WebRequest request, Map<String, String> errors) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(request != null ? request.getDescription(false).replace("uri=", "") : "")
                .errors(errors)
                .build();
        return new ResponseEntity<>(error, status);
    }
}





//package com.meethub.exception;
//
//import jakarta.validation.ConstraintViolationException;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.FieldError;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import org.springframework.web.context.request.WebRequest;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@Slf4j
//@RestControllerAdvice
//public class GlobalExceptionHandler {
//
//    // ✅ DTO dla odpowiedzi błędów
//    @lombok.Data
//    @lombok.Builder
//    public static class ErrorResponse {
//        private LocalDateTime timestamp;
//        private int status;
//        private String error;
//        private String message;
//        private String path;
//        private Map<String, String> errors;
//    }
//
//    @ExceptionHandler(ResourceNotFoundException.class)
//    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, WebRequest request) {
//        log.warn("Resource not found: {}", ex.getMessage());
//
//        ErrorResponse error = ErrorResponse.builder()
//                .timestamp(LocalDateTime.now())
//                .status(HttpStatus.NOT_FOUND.value())
//                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
//                .message(ex.getMessage())
//                .path(request.getDescription(false).replace("uri=", ""))
//                .build();
//
//        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
//    }
//
//    @ExceptionHandler(ValidationException.class)
//    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex, WebRequest request) {
//        log.warn("Validation error: {}", ex.getMessage());
//
//        ErrorResponse error = ErrorResponse.builder()
//                .timestamp(LocalDateTime.now())
//                .status(HttpStatus.BAD_REQUEST.value())
//                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
//                .message(ex.getMessage())
//                .path(request.getDescription(false).replace("uri=", ""))
//                .build();
//
//        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
//    }
//
//    @ExceptionHandler(BusinessException.class)
//    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, WebRequest request) {
//        log.warn("Business error: {}", ex.getMessage());
//
//        ErrorResponse error = ErrorResponse.builder()
//                .timestamp(LocalDateTime.now())
//                .status(HttpStatus.CONFLICT.value())
//                .error(HttpStatus.CONFLICT.getReasonPhrase())
//                .message(ex.getMessage())
//                .path(request.getDescription(false).replace("uri=", ""))
//                .build();
//
//        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
//    }
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ErrorResponse> handleValidationExceptions(
//            MethodArgumentNotValidException ex, WebRequest request) {
//
//        Map<String, String> errors = new HashMap<>();
//        ex.getBindingResult().getAllErrors().forEach(error -> {
//            String fieldName = error instanceof FieldError ?
//                    ((FieldError) error).getField() : error.getObjectName();
//            String errorMessage = error.getDefaultMessage();
//            errors.put(fieldName, errorMessage);
//        });
//
//        log.warn("Validation failed: {}", errors);
//
//        ErrorResponse error = ErrorResponse.builder()
//                .timestamp(LocalDateTime.now())
//                .status(HttpStatus.BAD_REQUEST.value())
//                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
//                .message("Walidacja danych nie powiodła się")
//                .path(request.getDescription(false).replace("uri=", ""))
//                .errors(errors)
//                .build();
//
//        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
//    }
//
//    @ExceptionHandler(IllegalArgumentException.class)
//    public ResponseEntity<ErrorResponse> handleIllegalArgument(
//            IllegalArgumentException ex, WebRequest request) {
//
//        log.warn("Illegal argument: {}", ex.getMessage());
//
//        ErrorResponse error = ErrorResponse.builder()
//                .timestamp(LocalDateTime.now())
//                .status(HttpStatus.BAD_REQUEST.value())
//                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
//                .message(ex.getMessage())
//                .path(request.getDescription(false).replace("uri=", ""))
//                .build();
//
//        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
//    }
//
//    @ExceptionHandler(IllegalStateException.class)
//    public ResponseEntity<ErrorResponse> handleIllegalState(
//            IllegalStateException ex, WebRequest request) {
//
//        log.warn("Illegal state: {}", ex.getMessage());
//
//        ErrorResponse error = ErrorResponse.builder()
//                .timestamp(LocalDateTime.now())
//                .status(HttpStatus.CONFLICT.value())
//                .error(HttpStatus.CONFLICT.getReasonPhrase())
//                .message(ex.getMessage())
//                .path(request.getDescription(false).replace("uri=", ""))
//                .build();
//
//        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
//        log.error("Unexpected error: {}", ex.getMessage(), ex);
//
//        ErrorResponse error = ErrorResponse.builder()
//                .timestamp(LocalDateTime.now())
//                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
//                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
//                .message("Wystąpił nieoczekiwany błąd serwera")
//                .path(request.getDescription(false).replace("uri=", ""))
//                .build();
//
//        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
//
//
//    @ExceptionHandler({ConstraintViolationException.class})
//    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
//        log.error("Validation error: {}", ex.getMessage(), ex);
//        String messages = ex.getConstraintViolations()
//                .stream()
//                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
//                .collect(Collectors.joining("; "));
//        return ResponseEntity.badRequest()
//                .body(new ErrorResponse("VALIDATION_ERROR", messages));
//    }
//
//    // Obsługa błędów walidacji argumentów w metodach kontrolera
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
//        log.error("Method argument validation error: {}", ex.getMessage(), ex);
//        String messages = ex.getBindingResult().getFieldErrors()
//                .stream()
//                .map(f -> f.getField() + ": " + f.getDefaultMessage())
//                .collect(Collectors.joining("; "));
//        return ResponseEntity.badRequest()
//                .body(new ErrorResponse("VALIDATION_ERROR", messages));
//    }
//}