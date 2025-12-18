package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.UserRole;
import com.meethub.domain.model.request.UserRegistrationRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
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
    }

    @Test
    void register_ShouldRegisterUser_WhenValidRequest() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        UserResponse result = authService.register(validRequest);

        assertAll("User registration",
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getEmail()).isEqualTo(validRequest.getEmail()),
                () -> assertThat(result.getFirstName()).isEqualTo(validRequest.getFirstName()),
                () -> assertThat(result.getLastName()).isEqualTo(validRequest.getLastName()),
                () -> assertThat(result.getRole()).isEqualTo(UserRole.PARTICIPANT)
        );

        verify(userRepository).existsByEmail(validRequest.getEmail());
        verify(passwordEncoder).encode(validRequest.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenEmailAlreadyExists() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Użytkownik z tym emailem już istnieje");

        verify(userRepository).existsByEmail(validRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_ShouldThrowException_WhenPasswordsDoNotMatch() {
        UserRegistrationRequest request = UserRegistrationRequest.builder()
                .email("test@example.com")
                .password("password123")
                .confirmPassword("differentPassword")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("123456789")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Hasła nie są identyczne");

        verify(userRepository).existsByEmail(request.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_ShouldSetDefaultRoleAndEnabledTrue() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("encodedPassword");

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

        UserResponse result = authService.register(validRequest);

        assertAll("Default user properties",
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getRole()).isEqualTo(UserRole.PARTICIPANT)
        );

        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_ShouldEncodePassword() {
        when(userRepository.existsByEmail(validRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(validRequest.getPassword())).thenReturn("encodedPassword123");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        authService.register(validRequest);

        verify(passwordEncoder).encode("password123");
    }
}
