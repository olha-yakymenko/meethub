package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.AttendanceToken;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.AttendanceTokenStatus;
import com.meethub.domain.repository.jpa.AttendanceTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class AttendanceTokenServiceImplIntegrationTest {

    @Autowired
    private AttendanceTokenServiceImpl attendanceTokenService;

    @MockBean
    private AttendanceTokenRepository attendanceTokenRepository;

    @Test
    void shouldCreateAndValidateToken_CompleteFlow() {
        // Given - User and meeting
        User user = User.builder()
                .id(1L)
                .firstName("Test")
                .email("test@example.com")
                .build();

        Meeting meeting = Meeting.builder()
                .title("Test Meeting")
                .startDate(LocalDateTime.now().plusHours(2))
                .organizer(user)
                .build();

        // Mock repository responses
        when(attendanceTokenRepository.findByUserIdAndMeetingId(anyLong(), anyLong()))
                .thenReturn(Optional.empty());

        AttendanceToken savedToken = AttendanceToken.builder()
                .id(1L)
                .token("generated_token_123")
                .user(user)
                .meeting(meeting)
                .status(AttendanceTokenStatus.ACTIVE)
                .expiresAt(meeting.getStartDate().plusHours(2))
                .build();

        when(attendanceTokenRepository.save(any(AttendanceToken.class)))
                .thenReturn(savedToken);

        // When - Create token
        AttendanceToken createdToken = attendanceTokenService.createToken(user, meeting);

        // Then - Verify creation
        assertAll("Token creation",
                () -> assertNotNull(createdToken),
                () -> assertEquals(user, createdToken.getUser()),
                () -> assertEquals(meeting, createdToken.getMeeting()),
                () -> assertEquals(AttendanceTokenStatus.ACTIVE, createdToken.getStatus())
        );

        // Given - Token validation
        when(attendanceTokenRepository.findByTokenAndMeetingId(
                eq("generated_token_123"), eq(1L)))
                .thenReturn(Optional.of(savedToken));

        when(attendanceTokenRepository.save(savedToken)).thenReturn(savedToken);

        // When - Validate and use token
        boolean isValid = attendanceTokenService.validateAndUseToken("generated_token_123", 1L);

        // Then - Verify validation
        assertAll("Token validation",
                () -> assertTrue(isValid, "Token should be valid"),
                () -> assertEquals(AttendanceTokenStatus.USED, savedToken.getStatus()),
                () -> assertNotNull(savedToken.getUsedAt())
        );
    }

    @Test
    void shouldHandleMultipleTokenOperations() {
        // Given
        User user1 = User.builder().id(1L).build();
        User user2 = User.builder().id(2L).build();

        Meeting meeting1 = Meeting.builder()
                .startDate(LocalDateTime.now().plusHours(1))
                .build();

        Meeting meeting2 = Meeting.builder()
                .startDate(LocalDateTime.now().plusHours(3))
                .build();

        // Mock different responses for different parameters
        when(attendanceTokenRepository.findByUserIdAndMeetingId(1L, 1L))
                .thenReturn(Optional.empty());

        when(attendanceTokenRepository.findByUserIdAndMeetingId(2L, 2L))
                .thenReturn(Optional.empty());

        AttendanceToken token1 = AttendanceToken.builder()
                .id(1L)
                .token("token1")
                .user(user1)
                .meeting(meeting1)
                .status(AttendanceTokenStatus.ACTIVE)
                .build();

        AttendanceToken token2 = AttendanceToken.builder()
                .id(2L)
                .token("token2")
                .user(user2)
                .meeting(meeting2)
                .status(AttendanceTokenStatus.ACTIVE)
                .build();

        when(attendanceTokenRepository.save(any(AttendanceToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When - Create tokens for different users/meetings
        AttendanceToken createdToken1 = attendanceTokenService.createToken(user1, meeting1);
        AttendanceToken createdToken2 = attendanceTokenService.createToken(user2, meeting2);

        // Then
        assertAll("Multiple tokens",
                () -> assertNotNull(createdToken1),
                () -> assertNotNull(createdToken2),
                () -> assertNotEquals(createdToken1.getToken(), createdToken2.getToken())
        );
    }

    @Test
    void shouldReturnActiveToken_WhenRequested() {
        // Given
        AttendanceToken activeToken = AttendanceToken.builder()
                .id(1L)
                .token("active_token")
                .status(AttendanceTokenStatus.ACTIVE)
                .build();

        when(attendanceTokenRepository.findActiveByUserAndMeeting(1L, 1L))
                .thenReturn(Optional.of(activeToken));

        // When
        Optional<AttendanceToken> result = attendanceTokenService
                .getTokenForUserAndMeeting(1L, 1L);

        // Then
        assertAll("Get active token",
                () -> assertTrue(result.isPresent()),
                () -> assertEquals("active_token", result.get().getToken()),
                () -> assertEquals(AttendanceTokenStatus.ACTIVE, result.get().getStatus())
        );
    }

    @Test
    void shouldNotReturnInactiveTokens() {
        // Given - Only inactive tokens exist
        AttendanceToken usedToken = AttendanceToken.builder()
                .id(1L)
                .token("used_token")
                .status(AttendanceTokenStatus.USED)
                .build();

        when(attendanceTokenRepository.findActiveByUserAndMeeting(1L, 1L))
                .thenReturn(Optional.empty());

        // When
        Optional<AttendanceToken> result = attendanceTokenService
                .getTokenForUserAndMeeting(1L, 1L);

        // Then
        assertFalse(result.isPresent(), "Should not return inactive tokens");
    }
}