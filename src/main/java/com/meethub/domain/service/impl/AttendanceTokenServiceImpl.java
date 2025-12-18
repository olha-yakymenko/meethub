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

    private String generateToken() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes)
                .replace("=", "")
                .replace("+", "-")
                .replace("/", "_")
                .substring(0, 32);
    }

    @Override
    public AttendanceToken createToken(User user, Meeting meeting) {

        deactivateOldTokens(user.getId(), meeting.getId());

        String token = generateToken();
        LocalDateTime expiresAt = meeting.getStartDate().plusHours(2);

        AttendanceToken attendanceToken = AttendanceToken.builder()
                .token(token)
                .user(user)
                .meeting(meeting)
                .expiresAt(expiresAt)
                .build();

        return attendanceTokenRepository.save(attendanceToken);
    }

    @Override
    public boolean validateAndUseToken(String token, Long meetingId) {
        try {
            Optional<AttendanceToken> tokenOpt =
                    attendanceTokenRepository.findByTokenAndMeetingId(token, meetingId);

            if (tokenOpt.isEmpty()) {
                log.warn("Token not found: {}", token);
                return false;
            }

            AttendanceToken attendanceToken = tokenOpt.get();

            if (attendanceToken.getStatus() != AttendanceTokenStatus.ACTIVE) {
                log.warn("Token not active: {}", token);
                return false;
            }

            if (LocalDateTime.now().isAfter(attendanceToken.getExpiresAt())) {
                log.warn("Token expired: {}", token);
                attendanceToken.setStatus(AttendanceTokenStatus.EXPIRED);
                attendanceTokenRepository.save(attendanceToken);
                return false;
            }

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

    @Override
    public Optional<AttendanceToken> getTokenForUserAndMeeting(
            Long userId, Long meetingId) {
        return attendanceTokenRepository.findActiveByUserAndMeeting(userId, meetingId);
    }

    private void deactivateOldTokens(Long userId, Long meetingId) {
        attendanceTokenRepository.findByUserIdAndMeetingId(userId, meetingId)
                .ifPresent(oldToken -> {
                    oldToken.setStatus(AttendanceTokenStatus.CANCELLED);
                    attendanceTokenRepository.save(oldToken);
                });
    }

    @Override
    public Optional<Long> getUserIdFromToken(String token, Long meetingId) {
        return attendanceTokenRepository.findByTokenAndMeetingId(token, meetingId)
                .filter(t -> t.getStatus() == AttendanceTokenStatus.ACTIVE)
                .map(t -> t.getUser().getId());
    }

}
