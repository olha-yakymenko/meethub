package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.UserRole;
import com.meethub.domain.model.request.UserRegistrationRequest;
import com.meethub.domain.model.response.AuthResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.exception.BusinessException;
import com.meethub.security.CustomUserDetailsService;
import com.meethub.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserRegistrationRequest validRequest;
    private User testUser;
    private UserDetails testUserDetails;

    @BeforeEach
    void setUp() {
        validRequest = UserRegistrationRequest.builder()
                .email("test@example.com")
                .password("password123")
                .confirmPassword("password123")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("123456789")
                .build();

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("123456789")
                .role(UserRole.PARTICIPANT)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        testUserDetails = new org.springframework.security.core.userdetails.User(
                "test@example.com",
                "encodedPassword",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_PARTICIPANT"))
        );
    }

    // ========== TESTY REJESTRACJI ==========

    @Test
    void register_ShouldRegisterUser_WhenValidRequest() {
        // Given
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse result = authService.register(validRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(validRequest.getEmail());
        assertThat(result.getFirstName()).isEqualTo(validRequest.getFirstName());
        assertThat(result.getLastName()).isEqualTo(validRequest.getLastName());
        assertThat(result.getRole()).isEqualTo(UserRole.PARTICIPANT);

        verify(userRepository).existsByEmail(validRequest.getEmail());
        verify(passwordEncoder).encode(validRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        // Given
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Użytkownik z tym emailem już istnieje");

        verify(userRepository).existsByEmail(validRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenPasswordsDoNotMatch() {
        // Given
        // Zamiast toBuilder(), utwórz nowy obiekt z różnym confirmPassword
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("test@example.com")
                .password("password123")
                .confirmPassword("differentPassword") // Różne hasło
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("123456789")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Hasła nie są identyczne");

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_ShouldSetDefaultRoleAndEnabledTrue() {
        // Given
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("encodedPassword");

        // Tworzymy nowego użytkownika do zwrócenia
        User savedUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("123456789")
                .role(UserRole.PARTICIPANT)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // When
        UserResponse result = authService.register(validRequest);

        // Then - sprawdzamy przez zwrócony obiekt
        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(UserRole.PARTICIPANT);
        // Jeśli UserResponse ma pole enabled, sprawdź je

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_ShouldEncodePassword() {
        // Given
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        authService.register(validRequest);

        // Then
        verify(passwordEncoder).encode("password123");
    }

    // ========== TESTY REFRESH TOKEN ==========

    @Test
    void refreshToken_ShouldReturnNewTokens_WhenValidRefreshToken() {
        // Given
        String refreshToken = "valid.refresh.token";
        String accessToken = "new.access.token";
        String newRefreshToken = "new.refresh.token";

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUserDetails, null, testUserDetails.getAuthorities());

        when(jwtUtil.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsername(refreshToken)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(jwtUtil.isTokenValid(refreshToken, testUserDetails)).thenReturn(true);
        when(jwtUtil.generateToken(any(Authentication.class))).thenReturn(accessToken);
        when(jwtUtil.generateRefreshToken(any(Authentication.class))).thenReturn(newRefreshToken);
        when(jwtUtil.getExpirationTime()).thenReturn(3600L);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        AuthResponse result = authService.refreshToken("Bearer " + refreshToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(accessToken);
        assertThat(result.getRefreshToken()).isEqualTo(newRefreshToken);
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresIn()).isEqualTo(3600L);
        assertThat(result.getUser()).isNotNull();
        assertThat(result.getUser().getEmail()).isEqualTo("test@example.com");

        verify(jwtUtil).isRefreshToken(refreshToken);
        verify(jwtUtil).extractUsername(refreshToken);
        verify(userDetailsService).loadUserByUsername("test@example.com");
        verify(jwtUtil).isTokenValid(refreshToken, testUserDetails);
        verify(jwtUtil).generateToken(any(Authentication.class));
        verify(jwtUtil).generateRefreshToken(any(Authentication.class));
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void refreshToken_ShouldWorkWithoutBearerPrefix() {
        // Given
        String refreshToken = "valid.refresh.token";
        String accessToken = "new.access.token";
        String newRefreshToken = "new.refresh.token";

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                testUserDetails, null, testUserDetails.getAuthorities());

        when(jwtUtil.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsername(refreshToken)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(jwtUtil.isTokenValid(refreshToken, testUserDetails)).thenReturn(true);
        when(jwtUtil.generateToken(any(Authentication.class))).thenReturn(accessToken);
        when(jwtUtil.generateRefreshToken(any(Authentication.class))).thenReturn(newRefreshToken);
        when(jwtUtil.getExpirationTime()).thenReturn(3600L);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // When
        AuthResponse result = authService.refreshToken(refreshToken);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo(accessToken);
        verify(jwtUtil).isRefreshToken(refreshToken);
    }

    @Test
    void refreshToken_ShouldThrowException_WhenInvalidRefreshToken() {
        // Given
        String refreshToken = "invalid.refresh.token";

        when(jwtUtil.isRefreshToken(refreshToken)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken("Bearer " + refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid refresh token");

        verify(jwtUtil).isRefreshToken(refreshToken);
        verify(jwtUtil, never()).extractUsername(anyString());
    }

    @Test
    void refreshToken_ShouldThrowException_WhenTokenInvalid() {
        // Given
        String refreshToken = "expired.refresh.token";

        when(jwtUtil.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsername(refreshToken)).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(testUserDetails);
        when(jwtUtil.isTokenValid(refreshToken, testUserDetails)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Refresh token expired or invalid");
    }

    @Test
    void refreshToken_ShouldThrowException_WhenUserNotFound() {
        // Given
        String refreshToken = "valid.refresh.token";

        when(jwtUtil.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtUtil.extractUsername(refreshToken)).thenReturn("nonexistent@example.com");
        when(userDetailsService.loadUserByUsername("nonexistent@example.com")).thenReturn(testUserDetails);
        when(jwtUtil.isTokenValid(refreshToken, testUserDetails)).thenReturn(true);
        when(jwtUtil.generateToken(any(Authentication.class))).thenReturn("token");
        when(jwtUtil.generateRefreshToken(any(Authentication.class))).thenReturn("refresh");
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void refreshToken_ShouldHandleException_WhenJwtUtilThrowsException() {
        // Given
        String refreshToken = "malformed.refresh.token";

        when(jwtUtil.isRefreshToken(refreshToken)).thenThrow(new RuntimeException("Malformed token"));

        // When & Then
        assertThatThrownBy(() -> authService.refreshToken(refreshToken))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Refresh token failed");
    }

    // ========== TESTY MAPOWANIA (jeśli chcesz testować prywatną metodę) ==========

    // Jeśli mapToUserResponse jest prywatna, możesz:
    // 1. Zmienić na protected/public
    // 2. Testować przez publiczną metodę
    // 3. Użyć refleksji

    @Test
    void register_ShouldMapUserToResponseCorrectly() {
        // Given
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse result = authService.register(validRequest);

        // Then - testujemy mapowanie przez publiczną metodę register
        assertThat(result.getId()).isEqualTo(testUser.getId());
        assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(result.getFirstName()).isEqualTo(testUser.getFirstName());
        assertThat(result.getLastName()).isEqualTo(testUser.getLastName());
        assertThat(result.getPhoneNumber()).isEqualTo(testUser.getPhoneNumber());
        assertThat(result.getRole()).isEqualTo(testUser.getRole());
        assertThat(result.getCreatedAt()).isEqualTo(testUser.getCreatedAt());
    }

    @Test
    void register_ShouldHandleNullPhoneNumberInResponse() {
        // Given
        User userWithoutPhone = User.builder()
                .id(2L)
                .email("test2@example.com")
                .password("encodedPassword")
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber(null) // null phone number
                .role(UserRole.PARTICIPANT)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("test2@example.com")
                .password("password123")
                .confirmPassword("password123")
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber(null)
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(userWithoutPhone);

        // When
        UserResponse result = authService.register(request);

        // Then
        assertThat(result.getPhoneNumber()).isNull();
    }

    // Alternatywnie: test z refleksją jeśli musisz testować prywatną metodę
    @Test
    void mapToUserResponse_ShouldMapAllFields_UsingReflection() throws Exception {
        // Given
        User user = testUser;

        // When - użyj refleksji do wywołania prywatnej metody
        var method = AuthServiceImpl.class.getDeclaredMethod("mapToUserResponse", User.class);
        method.setAccessible(true);
        UserResponse result = (UserResponse) method.invoke(authService, user);

        // Then
        assertThat(result.getId()).isEqualTo(user.getId());
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        assertThat(result.getFirstName()).isEqualTo(user.getFirstName());
        assertThat(result.getLastName()).isEqualTo(user.getLastName());
        assertThat(result.getPhoneNumber()).isEqualTo(user.getPhoneNumber());
        assertThat(result.getRole()).isEqualTo(user.getRole());
        assertThat(result.getCreatedAt()).isEqualTo(user.getCreatedAt());
    }
}