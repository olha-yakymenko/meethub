//
//
//////package com.meethub.controller.api;
//////
//////import com.meethub.domain.model.request.LoginRequest;
//////import com.meethub.domain.model.request.UserRegistrationRequest;
//////import com.meethub.domain.model.response.ApiResponse;
//////import com.meethub.domain.model.response.AuthResponse;
//////import com.meethub.domain.model.response.UserResponse;
//////import com.meethub.domain.service.AuthService;
//////import jakarta.validation.Valid;
//////import lombok.RequiredArgsConstructor;
//////import org.springframework.http.ResponseEntity;
//////import org.springframework.web.bind.annotation.*;
//////
//////@RestController
//////@RequestMapping("/api/v1/auth")
//////@RequiredArgsConstructor
//////public class AuthController {
//////
//////    private final AuthService authService;
//////
//////    @PostMapping("/register")
//////    public ResponseEntity<ApiResponse<UserResponse>> register(
//////            @Valid @RequestBody UserRegistrationRequest request) {
//////        UserResponse user = authService.register(request);
//////        return ResponseEntity.ok(ApiResponse.success("User registered successfully", user));
//////    }
//////
//////    @PostMapping("/login")
//////    public ResponseEntity<ApiResponse<AuthResponse>> login(
//////            @Valid @RequestBody LoginRequest request) {
//////        AuthResponse authResponse = authService.login(request);
//////        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
//////    }
//////
//////    @PostMapping("/refresh")
//////    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
//////            @RequestHeader("Authorization") String refreshToken) {
//////        AuthResponse authResponse = authService.refreshToken(refreshToken);
//////        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authResponse));
//////    }
//////}
////
////
////
////
////
//////do zmian
////package com.meethub.controller.api;
////
////import com.meethub.domain.model.request.LoginRequest;
////import com.meethub.domain.model.request.UserRegistrationRequest;
////import com.meethub.domain.model.response.ApiResponse;
////import com.meethub.domain.model.response.AuthResponse;
////import com.meethub.domain.model.response.UserResponse;
////import com.meethub.domain.service.AuthService;
////import com.meethub.security.JwtUtil;
////import jakarta.validation.Valid;
////import lombok.RequiredArgsConstructor;
////import org.springframework.http.ResponseEntity;
////import org.springframework.security.authentication.AuthenticationManager;
////import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
////import org.springframework.security.core.Authentication;
////import org.springframework.security.core.context.SecurityContextHolder;
////import org.springframework.web.bind.annotation.*;
////
////@RestController
////@RequestMapping("/api/v1/auth")
////@RequiredArgsConstructor
////public class AuthController {
////
////    private final AuthService authService;
////    private final JwtUtil jwtUtil;
////    private final AuthenticationManager authenticationManager;
////
////
////    @PostMapping("/register")
////    public ResponseEntity<ApiResponse<UserResponse>> register(
////            @Valid @RequestBody UserRegistrationRequest request) {
////        UserResponse user = authService.register(request);
////        return ResponseEntity.ok(ApiResponse.success("User registered successfully", user));
////    }
////
////
////    @PostMapping("/login")
////    public ResponseEntity<ApiResponse<AuthResponse>> login(
////            @Valid @RequestBody LoginRequest request) {
////
////        Authentication authentication = authenticationManager.authenticate(
////                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
////        );
////
////        SecurityContextHolder.getContext().setAuthentication(authentication);
////
////        String jwt = jwtUtil.generateToken(authentication);
////        String refreshToken = jwtUtil.generateRefreshToken(authentication);
////
////        AuthResponse authResponse = AuthResponse.builder()
////                .accessToken(jwt)
////                .refreshToken(refreshToken)
////                .tokenType("Bearer")
////                .expiresIn(jwtUtil.getExpirationTime())
////                .build();
////
////        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
////    }
////
////    // ✅ PRZYWRÓĆ refresh token
////    @PostMapping("/refresh")
////    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
////            @RequestHeader("Authorization") String refreshToken) {
////        // Implementacja refresh token
////        AuthResponse authResponse = authService.refreshToken(refreshToken);
////        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authResponse));
////    }
////}
//
//
//
//
//
////package com.meethub.controller.api;
////
////import com.meethub.domain.model.request.LoginRequest;
////import com.meethub.domain.model.request.UserRegistrationRequest;
////import com.meethub.domain.model.response.ApiResponse;
////import com.meethub.domain.model.response.AuthResponse;
////import com.meethub.domain.model.response.UserResponse;
////import com.meethub.domain.service.AuthService;
////import jakarta.validation.Valid;
////import lombok.RequiredArgsConstructor;
////import org.springframework.http.ResponseEntity;
////import org.springframework.web.bind.annotation.*;
////
////@RestController
////@RequestMapping("/api/v1/auth")
////@RequiredArgsConstructor
////public class AuthController {
////
////    private final AuthService authService;
////
////    @PostMapping("/register")
////    public ResponseEntity<ApiResponse<UserResponse>> register(
////            @Valid @RequestBody UserRegistrationRequest request) {
////        UserResponse user = authService.register(request);
////        return ResponseEntity.ok(ApiResponse.success("User registered successfully", user));
////    }
////
////    @PostMapping("/login")
////    public ResponseEntity<ApiResponse<AuthResponse>> login(
////            @Valid @RequestBody LoginRequest request) {
////        AuthResponse authResponse = authService.login(request);
////        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
////    }
////
////    @PostMapping("/refresh")
////    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
////            @RequestHeader("Authorization") String refreshToken) {
////        AuthResponse authResponse = authService.refreshToken(refreshToken);
////        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authResponse));
////    }
////}
//
//
//
//
//
//
//package com.meethub.controller.api;
//
//import com.meethub.domain.model.request.LoginRequest;
//import com.meethub.domain.model.request.UserRegistrationRequest;
//import com.meethub.domain.model.response.ApiResponse;
//import com.meethub.domain.model.response.AuthResponse;
//import com.meethub.domain.model.response.UserResponse;
//import com.meethub.domain.service.AuthService;
//import com.meethub.security.JwtUtil;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.ui.Model;
//import org.springframework.validation.BindingResult;
//import org.springframework.web.bind.annotation.*;
//import org.springframework.web.servlet.mvc.support.RedirectAttributes;
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//@RestController
//@RequestMapping("/api/v1/auth")
//@RequiredArgsConstructor
//public class AuthController {
//
//    private final AuthService authService;
//    private final JwtUtil jwtUtil;
//    private final AuthenticationManager authenticationManager;
//
//
//    @PostMapping("/register")
//    public String registerUser(
//            @Valid @ModelAttribute("registrationRequest") UserRegistrationRequest request,
//            BindingResult bindingResult,
//            Model model,
//            RedirectAttributes redirectAttributes) {
//
//        log.info("Registering user with email: {}", request.getEmail());
//
//        // Walidacja
//        if (bindingResult.hasErrors()) {
//            log.warn("Validation errors: {}", bindingResult.getAllErrors());
//            return "auth/register";
//        }
//
//        try {
//            // Rejestracja użytkownika
//            UserResponse user = authService.register(request);
//            log.info("User registered successfully: {}", user.getEmail());
//
//            // Komunikat sukcesu
//            redirectAttributes.addFlashAttribute("message",
//                    "Rejestracja udana! Możesz się teraz zalogować.");
//
//            return "redirect:/login";
//
//        } catch (Exception e) {
//            log.error("Registration failed", e);
//            model.addAttribute("error", e.getMessage());
//            return "auth/register";
//        }
//    }
//
//
//    @GetMapping("/login")
//    public String loginPage(
//            @RequestParam(value = "error", required = false) String error,
//            @RequestParam(value = "logout", required = false) String logout,
//            @RequestParam(value = "expired", required = false) String expired,
//            Model model) {
//
//        if (error != null) {
//            model.addAttribute("error", "Nieprawidłowy email lub hasło");
//        }
//
//        if (logout != null) {
//            model.addAttribute("message", "Zostałeś wylogowany pomyślnie");
//        }
//
//        if (expired != null) {
//            model.addAttribute("error", "Sesja wygasła, zaloguj się ponownie");
//        }
//
//        return "auth/login"; // templates/auth/login.html
//    }
//
//    // ✅ PRZYWRÓĆ refresh token
//    @PostMapping("/refresh")
//    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
//            @RequestHeader("Authorization") String refreshToken) {
//        // Implementacja refresh token
//        AuthResponse authResponse = authService.refreshToken(refreshToken);
//        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authResponse));
//    }
//}