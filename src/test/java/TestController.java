package com.meethub.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "MeetHub API is running!");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return response;
    }

    @GetMapping("/all-methods")
    public Map<String, String> testAllMethods() {
        Map<String, String> response = new HashMap<>();
        response.put("method", "GET");
        response.put("status", "OK");
        return response;
    }

    @PostMapping("/all-methods")
    public Map<String, String> testAllMethodsPost(@RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();
        response.put("method", "POST");
        response.put("status", "OK");
        response.put("received", request.get("test"));
        return response;
    }

    @PutMapping("/all-methods")
    public Map<String, String> testAllMethodsPut(@RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();
        response.put("method", "PUT");
        response.put("status", "OK");
        response.put("received", request.get("test"));
        return response;
    }

    @DeleteMapping("/all-methods")
    public ResponseEntity<Map<String, String>> testAllMethodsDelete() {
        Map<String, String> response = new HashMap<>();
        response.put("method", "DELETE");
        response.put("status", "OK");
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/all-methods")
    public Map<String, String> testAllMethodsPatch(@RequestBody Map<String, String> request) {
        Map<String, String> response = new HashMap<>();
        response.put("method", "PATCH");
        response.put("status", "OK");
        response.put("received", request.get("test"));
        return response;
    }

    @GetMapping("/error-test")
    public ResponseEntity<Map<String, String>> errorTest() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ERROR");
        response.put("message", "This is a test error");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @GetMapping("/auth-test")
    public Map<String, String> authTest() {
        Map<String, String> response = new HashMap<>();
        response.put("endpoint", "Authentication Test");
        response.put("status", "OK - No auth required");
        return response;
    }
}