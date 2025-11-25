////package com.meethub.domain.service.impl;
////
////import com.meethub.domain.model.entity.User;
////import com.meethub.domain.model.enums.UserRole;
////import com.meethub.domain.model.request.LoginRequest;
////import com.meethub.domain.model.request.UserRegistrationRequest;
////import com.meethub.domain.model.response.AuthResponse;
////import com.meethub.domain.model.response.UserResponse;
////import com.meethub.domain.repository.jpa.UserRepository;
////import com.meethub.domain.service.AuthService;
////import com.meethub.exception.BusinessException;
////import com.meethub.security.JwtUtil;
////import lombok.RequiredArgsConstructor;
////import lombok.extern.slf4j.Slf4j;
////import org.springframework.security.authentication.AuthenticationManager;
////import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
////import org.springframework.security.core.Authentication;
////import org.springframework.security.crypto.password.PasswordEncoder;
////import org.springframework.stereotype.Service;
////import org.springframework.transaction.annotation.Transactional;
////
////@Service
////@RequiredArgsConstructor
////@Slf4j
////public class AuthServiceImpl implements AuthService {
////
////    private final UserRepository userRepository;
////    private final PasswordEncoder passwordEncoder;
////    private final AuthenticationManager authenticationManager;
////    private final JwtUtil jwtUtil;
////
////    @Override
////    @Transactional
////    public UserResponse register(UserRegistrationRequest request) {
////        // Sprawdź czy użytkownik już istnieje
////        if (userRepository.existsByEmail(request.getEmail())) {
////            throw new BusinessException("User with this email already exists");
////        }
////
////        // Utwórz nowego użytkownika
////        User user = new User();
////        user.setEmail(request.getEmail());
////        user.setPassword(passwordEncoder.encode(request.getPassword()));
////        user.setFirstName(request.getFirstName());
////        user.setLastName(request.getLastName());
////        user.setPhoneNumber(request.getPhoneNumber());
////        user.setRole(UserRole.PARTICIPANT); // Domyślna rola
////        user.setEnabled(true);
////
////        User savedUser = userRepository.save(user);
////        log.info("User registered successfully: {}", savedUser.getEmail());
////
////        // Mapowanie do response
////        return mapToUserResponse(savedUser);
////    }
////
////    @Override
////    public AuthResponse login(LoginRequest request) {
////        try {
////            // Uwierzytelnianie przez Spring Security
////            Authentication authentication = authenticationManager.authenticate(
////                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
////            );
////
////            // Generuj tokeny
////            String accessToken = jwtUtil.generateToken(authentication);
////            String refreshToken = jwtUtil.generateRefreshToken(authentication);
////
////            // Pobierz dane użytkownika
////            User user = userRepository.findByEmail(request.getEmail())
////                    .orElseThrow(() -> new BusinessException("User not found"));
////
////            return AuthResponse.builder()
////                    .accessToken(accessToken)
////                    .refreshToken(refreshToken)
////                    .tokenType("Bearer")
////                    .expiresIn(jwtUtil.getExpirationTime())
////                    .user(mapToUserResponse(user))
////                    .build();
////
////        } catch (Exception e) {
////            log.error("Login failed for user: {}", request.getEmail(), e);
////            throw new BusinessException("Invalid email or password");
////        }
////    }
////
////    @Override
////    public AuthResponse refreshToken(String refreshToken) {
////        // TODO: Implement refresh token logic
////        throw new BusinessException("Refresh token not implemented yet");
////    }
////
////    @Override
////    public void logout(String token) {
////        // TODO: Implement logout logic (blacklist token)
////        log.info("User logged out");
////    }
////
////    private UserResponse mapToUserResponse(User user) {
////        UserResponse response = new UserResponse();
////        response.setId(user.getId());
////        response.setEmail(user.getEmail());
////        response.setFirstName(user.getFirstName());
////        response.setLastName(user.getLastName());
////        response.setRole(user.getRole());
////        response.setPhoneNumber(user.getPhoneNumber());
////        response.setCreatedAt(user.getCreatedAt());
////        return response;
////    }
////}
//
//
//
//package com.meethub.domain.service.impl;
//
//import com.meethub.domain.model.entity.User;
//import com.meethub.domain.model.enums.UserRole;
//import com.meethub.domain.model.request.LoginRequest;
//import com.meethub.domain.model.request.UserRegistrationRequest;
//import com.meethub.domain.model.response.AuthResponse;
//import com.meethub.domain.model.response.UserResponse;
//import com.meethub.domain.repository.jpa.UserRepository;
//import com.meethub.domain.service.AuthService;
//import com.meethub.exception.BusinessException;
//import com.meethub.security.JwtUtil;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class AuthServiceImpl implements AuthService {
//
//    private final UserRepository userRepository;
//    private final PasswordEncoder passwordEncoder;
//    private final AuthenticationManager authenticationManager;
//    private final JwtUtil jwtUtil;
//
//    @Override
//    @Transactional
//    public UserResponse register(UserRegistrationRequest request) {
//        // Sprawdź czy użytkownik już istnieje
//        if (userRepository.existsByEmail(request.getEmail())) {
//            throw new BusinessException("Użytkownik z tym emailem już istnieje");
//        }
//
//        // Sprawdź czy hasła się zgadzają
//        if (!request.getPassword().equals(request.getConfirmPassword())) {
//            throw new BusinessException("Hasła nie są identyczne");
//        }
//
//        // Utwórz nowego użytkownika
//        User user = new User();
//        user.setEmail(request.getEmail());
//        user.setPassword(passwordEncoder.encode(request.getPassword()));
//        user.setFirstName(request.getFirstName());
//        user.setLastName(request.getLastName());
//        user.setPhoneNumber(request.getPhoneNumber());
//        user.setRole(UserRole.PARTICIPANT); // Domyślna rola
//        user.setEnabled(true);
//
//        User savedUser = userRepository.save(user);
//        log.info("User registered successfully: {}", savedUser.getEmail());
//
//        return mapToUserResponse(savedUser);
//    }
//
//    @Override
//    public AuthResponse login(LoginRequest request) {
//        try {
//            // Uwierzytelnianie przez Spring Security
//            Authentication authentication = authenticationManager.authenticate(
//                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
//            );
//
//            // Generuj tokeny
//            String accessToken = jwtUtil.generateToken(authentication);
//            String refreshToken = jwtUtil.generateRefreshToken(authentication);
//
//            // Pobierz dane użytkownika
//            User user = userRepository.findByEmail(request.getEmail())
//                    .orElseThrow(() -> new BusinessException("User not found"));
//
//            return AuthResponse.builder()
//                    .accessToken(accessToken)
//                    .refreshToken(refreshToken)
//                    .tokenType("Bearer")
//                    .expiresIn(jwtUtil.getExpirationTime())
//                    .user(mapToUserResponse(user))
//                    .build();
//
//        } catch (Exception e) {
//            log.error("Login failed for user: {}", request.getEmail(), e);
//            throw new BusinessException("Nieprawidłowy email lub hasło");
//        }
//    }
//
//    @Override
//    public AuthResponse refreshToken(String refreshToken) {
//        // TODO: Implement refresh token logic
//        throw new BusinessException("Refresh token not implemented yet");
//    }
//
//    @Override
//    public void logout(String token) {
//        // TODO: Implement logout logic (blacklist token)
//        log.info("User logged out");
//    }
//
//    private UserResponse mapToUserResponse(User user) {
//        UserResponse response = new UserResponse();
//        response.setId(user.getId());
//        response.setEmail(user.getEmail());
//        response.setFirstName(user.getFirstName());
//        response.setLastName(user.getLastName());
//        response.setRole(user.getRole());
//        response.setPhoneNumber(user.getPhoneNumber());
//        response.setCreatedAt(user.getCreatedAt());
//        return response;
//    }
//}



package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.UserRole;
import com.meethub.domain.model.request.UserRegistrationRequest;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.domain.service.AuthService;
import com.meethub.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse register(UserRegistrationRequest request) {
        // Sprawdź czy użytkownik już istnieje
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Użytkownik z tym emailem już istnieje");
        }

        // Sprawdź czy hasła się zgadzają
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("Hasła nie są identyczne");
        }

        // Utwórz nowego użytkownika
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setRole(UserRole.PARTICIPANT); // Domyślna rola
        user.setEnabled(true);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        return mapToUserResponse(savedUser);
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRole(user.getRole());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}