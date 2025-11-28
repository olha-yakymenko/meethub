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

import com.meethub.domain.model.request.LoginRequest;
import com.meethub.domain.model.request.UserRegistrationRequest;
import com.meethub.domain.model.response.ApiResponse;
import com.meethub.domain.model.response.AuthResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.service.AuthService;
import com.meethub.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;


    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody UserRegistrationRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", user));
    }


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtil.generateToken(authentication);
        String refreshToken = jwtUtil.generateRefreshToken(authentication);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(jwt)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationTime())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    // ✅ PRZYWRÓĆ refresh token
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestHeader("Authorization") String refreshToken) {
        // Implementacja refresh token
        AuthResponse authResponse = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authResponse));
    }
}