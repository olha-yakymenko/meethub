//package com.meethub.controller.api;
//
//import com.meethub.domain.model.request.LoginRequest;
//import com.meethub.domain.model.request.UserRegistrationRequest;
//import com.meethub.domain.model.response.ApiResponse;
//import com.meethub.domain.model.response.AuthResponse;
//import com.meethub.domain.model.response.UserResponse;
//import com.meethub.domain.service.AuthService;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/v1/auth")
//@RequiredArgsConstructor
//public class AuthController {
//
//    private final AuthService authService;
//
//    @PostMapping("/register")
//    public ResponseEntity<ApiResponse<UserResponse>> register(
//            @Valid @RequestBody UserRegistrationRequest request) {
//        UserResponse user = authService.register(request);
//        return ResponseEntity.ok(ApiResponse.success("User registered successfully", user));
//    }
//
//    @PostMapping("/login")
//    public ResponseEntity<ApiResponse<AuthResponse>> login(
//            @Valid @RequestBody LoginRequest request) {
//        AuthResponse authResponse = authService.login(request);
//        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
//    }
//
//    @PostMapping("/refresh")
//    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
//            @RequestHeader("Authorization") String refreshToken) {
//        AuthResponse authResponse = authService.refreshToken(refreshToken);
//        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authResponse));
//    }
//}






package com.meethub.controller.api;

import com.meethub.domain.model.request.UserRegistrationRequest;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody UserRegistrationRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", user));
    }

    // USUŃ login i refreshToken - Spring Security obsługuje logowanie przez formularz
}