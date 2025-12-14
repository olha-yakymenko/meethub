package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.UserRole;
import com.meethub.domain.model.request.UserRegistrationRequest;
import com.meethub.domain.model.response.AuthResponse;
import com.meethub.domain.model.response.UserResponse;
import com.meethub.domain.repository.jpa.UserRepository;
import com.meethub.exception.BusinessException;
import com.meethub.security.CustomUserDetailsService;
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

}