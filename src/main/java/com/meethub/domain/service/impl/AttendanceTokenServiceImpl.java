package com.meethub.domain.service.impl;

import com.meethub.domain.model.entity.AttendanceToken;
import com.meethub.domain.model.entity.Meeting;
import com.meethub.domain.model.entity.User;
import com.meethub.domain.model.enums.AttendanceTokenStatus;
import com.meethub.domain.repository.jpa.AttendanceTokenRepository;
import com.meethub.domain.service.AttendanceTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceTokenServiceImpl implements AttendanceTokenService {

    private final AttendanceTokenRepository attendanceTokenRepository;
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    /**
     * Generuje unikalny token
     */

    private String generateToken() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes)
                .replace("=", "")
                .replace("+", "-")
                .replace("/", "_")
                .substring(0, 32);
    }

    /**
     * Tworzy nowy token dla użytkownika i spotkania
     */
    @Override
    public AttendanceToken createToken(User user, Meeting meeting) {
        // Dezaktywuj stare tokeny dla tego użytkownika i spotkania
        deactivateOldTokens(user.getId(), meeting.getId());

        String token = generateToken();
        LocalDateTime expiresAt = meeting.getStartDate().plusHours(2); // Ważny 2h po rozpoczęciu

        AttendanceToken attendanceToken = AttendanceToken.builder()
                .token(token)
                .user(user)
                .meeting(meeting)
                .expiresAt(expiresAt)
                .build();

        return attendanceTokenRepository.save(attendanceToken);
    }

    /**
     * Waliduje i używa tokenu
     */
    public boolean validateAndUseToken(String token, Long meetingId) {
        try {
            Optional<AttendanceToken> tokenOpt = attendanceTokenRepository
                    .findByTokenAndMeetingId(token, meetingId);

            if (tokenOpt.isEmpty()) {
                log.warn("Token not found: {}", token);
                return false;
            }

            AttendanceToken attendanceToken = tokenOpt.get();

            // Sprawdź czy token jest aktywny
            if (attendanceToken.getStatus() != AttendanceTokenStatus.ACTIVE) {
                log.warn("Token not active: {}", token);
                return false;
            }

            // Sprawdź czy token nie wygasł
            if (LocalDateTime.now().isAfter(attendanceToken.getExpiresAt())) {
                log.warn("Token expired: {}", token);
                attendanceToken.setStatus(AttendanceTokenStatus.EXPIRED);
                attendanceTokenRepository.save(attendanceToken);
                return false;
            }

            // Oznacz jako użyty
            attendanceToken.setStatus(AttendanceTokenStatus.USED);
            attendanceToken.setUsedAt(LocalDateTime.now());
            attendanceTokenRepository.save(attendanceToken);

            log.info("Token used successfully: {} for meeting: {}", token, meetingId);
            return true;

        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Pobiera token dla użytkownika i spotkania
     */
    @Override
    public Optional<AttendanceToken> getTokenForUserAndMeeting(Long userId, Long meetingId) {
        return attendanceTokenRepository.findActiveByUserAndMeeting(userId, meetingId);
    }

    /**
     * Dezaktywuje stare tokeny
     */
    private void deactivateOldTokens(Long userId, Long meetingId) {
        attendanceTokenRepository.findByUserIdAndMeetingId(userId, meetingId)
                .ifPresent(oldToken -> {
                    oldToken.setStatus(AttendanceTokenStatus.CANCELLED);
                    attendanceTokenRepository.save(oldToken);
                });
    }
}