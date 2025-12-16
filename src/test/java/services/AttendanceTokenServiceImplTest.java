package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.AttendanceToken;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.AttendanceTokenStatus;
import com.meethub.domain.repository.jpa.AttendanceTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceTokenServiceImplTest {

    @Mock
    private AttendanceTokenRepository attendanceTokenRepository;

    @InjectMocks
    private AttendanceTokenServiceImpl attendanceTokenService;

    private User testUser;
    private Meeting testMeeting;
    private AttendanceToken existingToken;
    private AttendanceToken newToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .firstName("Jan")
                .lastName("Kowalski")
                .email("jan.kowalski@example.com")
                .build();

        testMeeting = Meeting.builder()
                .title("Spotkanie testowe")
                .startDate(LocalDateTime.now().plusHours(1))
                .organizer(testUser)
                .build();

        existingToken = AttendanceToken.builder()
                .id(1L)
                .token("old_token_123")
                .user(testUser)
                .meeting(testMeeting)
                .status(AttendanceTokenStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusHours(3))
                .build();

        newToken = AttendanceToken.builder()
                .id(2L)
                .token("new_token_456")
                .user(testUser)
                .meeting(testMeeting)
                .status(AttendanceTokenStatus.ACTIVE)
                .expiresAt(testMeeting.getStartDate().plusHours(2))
                .build();
    }



    @Test
    void shouldValidateAndUseToken_Successfully() {
        // Given
        String validToken = "valid_token_123";
        LocalDateTime futureExpiry = LocalDateTime.now().plusHours(1);

        AttendanceToken activeToken = AttendanceToken.builder()
                .id(1L)
                .token(validToken)
                .user(testUser)
                .meeting(testMeeting)
                .status(AttendanceTokenStatus.ACTIVE)
                .expiresAt(futureExpiry)
                .build();

        when(attendanceTokenRepository.findByTokenAndMeetingId(
                eq(validToken), eq(1L)))
                .thenReturn(Optional.of(activeToken));

        when(attendanceTokenRepository.save(any(AttendanceToken.class)))
                .thenReturn(activeToken);

        // When
        boolean isValid = attendanceTokenService.validateAndUseToken(validToken, 1L);

        // Then
        assertAll("Token validation success",
                () -> assertTrue(isValid, "Valid token should return true"),
                () -> assertEquals(AttendanceTokenStatus.USED, activeToken.getStatus(),
                        "Token status should be USED"),
                () -> assertNotNull(activeToken.getUsedAt(), "UsedAt timestamp should be set")
        );

        verify(attendanceTokenRepository, times(1)).save(activeToken);
    }

    @Test
    void shouldReturnFalse_WhenTokenNotFound() {
        // Given
        String invalidToken = "non_existent_token";

        when(attendanceTokenRepository.findByTokenAndMeetingId(
                eq(invalidToken), eq(1L)))
                .thenReturn(Optional.empty());

        // When
        boolean isValid = attendanceTokenService.validateAndUseToken(invalidToken, 1L);

        // Then
        assertFalse(isValid, "Non-existent token should return false");
        verify(attendanceTokenRepository, never()).save(any());
    }

    @Test
    void shouldReturnFalse_WhenTokenNotActive() {
        // Given
        String usedToken = "used_token_123";

        AttendanceToken usedAttendanceToken = AttendanceToken.builder()
                .id(1L)
                .token(usedToken)
                .status(AttendanceTokenStatus.USED)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(attendanceTokenRepository.findByTokenAndMeetingId(
                eq(usedToken), eq(1L)))
                .thenReturn(Optional.of(usedAttendanceToken));

        // When
        boolean isValid = attendanceTokenService.validateAndUseToken(usedToken, 1L);

        // Then
        assertFalse(isValid, "Used token should return false");
        verify(attendanceTokenRepository, never()).save(any());
    }

    @Test
    void shouldReturnFalse_WhenTokenExpired() {
        // Given
        String expiredToken = "expired_token_123";
        LocalDateTime pastExpiry = LocalDateTime.now().minusHours(1);

        AttendanceToken expiredAttendanceToken = AttendanceToken.builder()
                .id(1L)
                .token(expiredToken)
                .status(AttendanceTokenStatus.ACTIVE)
                .expiresAt(pastExpiry)
                .build();

        when(attendanceTokenRepository.findByTokenAndMeetingId(
                eq(expiredToken), eq(1L)))
                .thenReturn(Optional.of(expiredAttendanceToken));

        when(attendanceTokenRepository.save(any(AttendanceToken.class)))
                .thenReturn(expiredAttendanceToken);

        // When
        boolean isValid = attendanceTokenService.validateAndUseToken(expiredToken, 1L);

        // Then
        assertAll("Expired token validation",
                () -> assertFalse(isValid, "Expired token should return false"),
                () -> assertEquals(AttendanceTokenStatus.EXPIRED,
                        expiredAttendanceToken.getStatus(), "Token should be marked as EXPIRED")
        );

        verify(attendanceTokenRepository, times(1)).save(expiredAttendanceToken);
    }

    @Test
    void shouldReturnFalse_WhenRepositoryThrowsException() {
        // Given
        String token = "test_token";

        when(attendanceTokenRepository.findByTokenAndMeetingId(
                eq(token), eq(1L)))
                .thenThrow(new RuntimeException("Database error"));

        // When
        boolean isValid = attendanceTokenService.validateAndUseToken(token, 1L);

        // Then
        assertFalse(isValid, "Should return false on exception");
    }

    @Test
    void shouldGetTokenForUserAndMeeting_Successfully() {
        // Given
        when(attendanceTokenRepository.findActiveByUserAndMeeting(
                eq(1L), eq(1L)))
                .thenReturn(Optional.of(existingToken));

        // When
        Optional<AttendanceToken> result = attendanceTokenService
                .getTokenForUserAndMeeting(1L, 1L);

        // Then
        assertAll("Get token validation",
                () -> assertTrue(result.isPresent(), "Token should be present"),
                () -> assertEquals(existingToken, result.get(), "Should return correct token")
        );
    }

    @Test
    void shouldReturnEmptyOptional_WhenNoActiveToken() {
        // Given
        when(attendanceTokenRepository.findActiveByUserAndMeeting(
                eq(1L), eq(1L)))
                .thenReturn(Optional.empty());

        // When
        Optional<AttendanceToken> result = attendanceTokenService
                .getTokenForUserAndMeeting(1L, 1L);

        // Then
        assertFalse(result.isPresent(), "Should return empty optional when no token");
    }


    @Test
    void shouldLogAppropriately_ForDifferentScenarios() {
        // Test that appropriate logging happens
        // This is more of an integration concern, but we can verify method calls

        String validToken = "valid_token";
        AttendanceToken activeToken = AttendanceToken.builder()
                .token(validToken)
                .status(AttendanceTokenStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(attendanceTokenRepository.findByTokenAndMeetingId(
                eq(validToken), eq(1L)))
                .thenReturn(Optional.of(activeToken));

        when(attendanceTokenRepository.save(any(AttendanceToken.class)))
                .thenReturn(activeToken);

        // When
        boolean result = attendanceTokenService.validateAndUseToken(validToken, 1L);

        // Then - Method should complete without exception
        assertTrue(result);
        // Log verification would require a different approach (e.g., using @Capture)
    }
}