package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.UserRole;
import com.meethub.domain.model.request.UserRegistrationRequest;
import com.meethub.domain.model.response.AuthResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.domain.service.AuthService;
import com.meethub.exception.BusinessException;
import com.meethub.security.CustomUserDetailsService;
import com.meethub.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

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

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        try {
            if (refreshToken.startsWith("Bearer ")) {
                refreshToken = refreshToken.substring(7);
            }

            if (!jwtUtil.isRefreshToken(refreshToken)) {
                throw new BusinessException("Invalid refresh token");
            }

            String userEmail = jwtUtil.extractUsername(refreshToken);
            var userDetails = userDetailsService.loadUserByUsername(userEmail);

            if (!jwtUtil.isTokenValid(refreshToken, userDetails)) {
                throw new BusinessException("Refresh token expired or invalid");
            }

            // ✅ TWORZYMY Authentication object
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());

            // ✅ TERAZ MOŻEMY UŻYĆ ISTNIEJĄCYCH METOD
            String newAccessToken = jwtUtil.generateToken(authentication);
            String newRefreshToken = jwtUtil.generateRefreshToken(authentication);

            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new BusinessException("User not found"));

            return AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtUtil.getExpirationTime())
                    .user(mapToUserResponse(user))
                    .build();

        } catch (Exception e) {
            log.error("Refresh token failed: {}", e.getMessage());
            throw new BusinessException("Refresh token failed: " + e.getMessage());
        }
    }
}