package com.meethub.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;

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
